#!/usr/bin/env python3
"""Run against a disposable PostgreSQL-backed app; creates test users and content."""
import json, os, urllib.request, urllib.error, uuid, concurrent.futures
BASE=os.environ.get('VCRAFT_URL','http://localhost:8080')
results=[]
http_calls=0
def req(method,path,data=None,token=None,status=200):
    global http_calls
    http_calls+=1
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    request=urllib.request.Request(BASE+'/api/v1'+path, data=json.dumps(data).encode() if data is not None else None, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as r: code,raw=r.status,r.read()
    except urllib.error.HTTPError as e: code,raw=e.code,e.read()
    value=json.loads(raw) if raw else None
    assert code==status,(method,path,code,status,value)
    results.append(f'{method} {path} -> {code}')
    return value
suffix=uuid.uuid4().hex[:10]
admin=req('POST','/auth/login',{'email':'admin@vcraft.vn','password':'uet123456'})['accessToken']
buyer_auth=req('POST','/auth/register/buyer',{'name':'Smoke Buyer','email':suffix+'@test.vn','password':'Password123!','phone':'0901234567'},status=201)
buyer=buyer_auth['accessToken']
a=req('POST','/auth/register/artisan',{'workshopName':'Smoke Workshop','ownerName':'Tester','email':'a'+suffix+'@test.vn','password':'Password123!','address':'Bat Trang','lat':20.97,'lng':105.91},status=201)

artisan_id=a['id']
artisan=req('POST','/auth/login',{'email':'a'+suffix+'@test.vn','password':'Password123!'})['accessToken']
req('POST','/batches',{'name':'Denied','quantity':5},artisan,status=403)
req('PATCH',f'/admin/artisans/{artisan_id}/approve',token=admin)
req('PUT','/artisans/me',{'familyHistory':'Three generations of ceramics','images':[],'certificates':['Quality certificate'],'latitude':20.97,'longitude':105.91},artisan)
batch=req('POST','/batches',{'name':'Smoke batch','description':'Traceable ceramics','quantity':100},artisan,status=201)
log=req('POST',f"/batches/{batch['id']}/logs",{'stage':'Nung lò','note':'1200°C','images':[]},artisan,status=201)
product=req('POST','/products',{'name':'Smoke cup','description':'Handmade','price':10000,'stock':100,'batchId':batch['id'],'images':[]},artisan,status=201)
updated=req('PUT','/products/'+product['id'],{'name':'Updated cup'},artisan)
assert updated['stock']==100
story=req('GET','/trace/'+batch['id']); assert len(story['logs'])==1 and len(story['products'])==1
pins=req('GET','/map/pins'); assert any(p['id']==artisan_id for p in pins)
quote=req('POST','/quotes',{'title':'100 cups','description':'Logo cups','quantity':100,'targetArtisanIds':[artisan_id]},buyer,status=201)
req('PATCH',f"/quotes/{quote['id']}/submit",token=buyer)
incoming=req('GET','/quotes/artisan/incoming',token=artisan); assert any(q['id']==quote['id'] for q in incoming)
bid=req('POST',f"/quotes/{quote['id']}/bids",{'pricePerUnit':10000,'estimatedDays':20},artisan,status=201)
req('PATCH',f"/quotes/{quote['id']}/bids/{bid['id']}/accept",token=buyer)
assert req('GET','/quotes/'+quote['id'],token=buyer)['quote']['status']=='ACCEPTED'
req('PATCH',f"/quotes/{quote['id']}/start-production",token=artisan)
req('PATCH',f"/quotes/{quote['id']}/complete",{'deliveryNote':'Done','deliveryImages':[]},artisan)
req('PATCH',f"/quotes/{quote['id']}/confirm-delivery",token=buyer)
assert req('GET','/quotes/'+quote['id'],token=buyer)['quote']['status']=='CLOSED'
users=req('GET','/admin/users',token=admin)
assert all('passwordHash' not in u for u in users['data'])
# A second approved workshop must not receive or bid on a private request.
b=req('POST','/auth/register/artisan',{'workshopName':'Second Workshop','ownerName':'Other','email':'b'+suffix+'@test.vn','password':'Password123!','address':'Bat Trang'},status=201)
second=req('POST','/auth/login',{'email':'b'+suffix+'@test.vn','password':'Password123!'})['accessToken']
req('PATCH',f"/admin/artisans/{b['id']}/approve",token=admin)
private=req('POST','/quotes',{'title':'Private','description':'Private cups','quantity':100,'targetArtisanIds':[artisan_id]},buyer,status=201)
req('PATCH',f"/quotes/{private['id']}/submit",token=buyer)
assert not any(q['id']==private['id'] for q in req('GET','/quotes/artisan/incoming',token=second))
req('POST',f"/quotes/{private['id']}/bids",{'pricePerUnit':100,'estimatedDays':2},second,status=403)
# Concurrent acceptance: exactly one winner and one conflict, no partial bid writes.
race=req('POST','/quotes',{'title':'Race','description':'Two workshops','quantity':100,'targetArtisanIds':[artisan_id,b['id']]},buyer,status=201)
req('PATCH',f"/quotes/{race['id']}/submit",token=buyer)
bids=[req('POST',f"/quotes/{race['id']}/bids",{'pricePerUnit':100,'estimatedDays':2},t,status=201) for t in (artisan,second)]
def accept(bid):
    path=f"/quotes/{race['id']}/bids/{bid['id']}/accept"
    request=urllib.request.Request(BASE+'/api/v1'+path,headers={'Authorization':'Bearer '+buyer},method='PATCH')
    try:
        with urllib.request.urlopen(request) as r: return r.status
    except urllib.error.HTTPError as e: return e.code
with concurrent.futures.ThreadPoolExecutor(2) as pool:
    codes=list(pool.map(accept,bids))
http_calls+=len(codes)
assert sorted(codes)==[200,409],codes
race_detail=req('GET','/quotes/'+race['id'],token=buyer)
assert sum(x['status']=='ACCEPTED' for x in race_detail['bids'])==1
assert sum(x['status']=='REJECTED' for x in race_detail['bids'])==1
# Cross-batch log changes are rejected even for the same workshop.
other_batch=req('POST','/batches',{'name':'Other batch','quantity':10},artisan,status=201)
req('PUT',f"/batches/{other_batch['id']}/logs/{log['id']}",{'note':'Wrong lot'},artisan,status=403)
req('DELETE',f"/batches/{other_batch['id']}/logs/{log['id']}",token=artisan,status=403)
# BQL splits an unawarded request; all workshop lots preserve the original buyer.
parent=req('POST','/quotes',{'title':'Large order','description':'Split lots','quantity':100},buyer,status=201)
parts=[{'artisanId':artisan_id,'quantity':60},{'artisanId':b['id'],'quantity':40}]
children=req('POST',f"/quotes/{parent['id']}/allocations",parts,admin,status=201)
assert sum(c['quantity'] for c in children)==100
assert len(req('GET',f"/quotes/{parent['id']}/allocations",token=buyer))==2
assert req('GET','/quotes/'+parent['id'],token=buyer)['quote']['status']=='ALLOCATED'
req('GET','/products'); req('GET','/artisans'); req('GET','/news'); req('GET','/admin/stats',token=admin)
# Multiple images are optimized and served with matching JPEG encoding.
import io
from PIL import Image
buf=io.BytesIO(); Image.new('RGB',(2400,1600),(170,90,40)).save(buf,format='PNG')
boundary='vcraft'+suffix
body=b''
for name in ('first.png','second.webp'):
    body+=('--'+boundary+'\r\nContent-Disposition: form-data; name="files"; filename="'+name+'"\r\nContent-Type: image/png\r\n\r\n').encode()+buf.getvalue()+b'\r\n'
body+=('--'+boundary+'--\r\n').encode()
request=urllib.request.Request(BASE+'/api/v1/products/'+product['id']+'/images',data=body,headers={'Authorization':'Bearer '+artisan,'Content-Type':'multipart/form-data; boundary='+boundary},method='POST')
http_calls+=1
with urllib.request.urlopen(request) as r: urls=json.load(r)
assert len(urls)==2
for url in urls:
    http_calls+=1
    with urllib.request.urlopen(url) as r: image=Image.open(io.BytesIO(r.read())); assert image.format=='JPEG' and max(image.size)<=1200
results.append('Multipart upload: two optimized JPEG images, publicly readable')
# Retail inventory is reserved on checkout and restored once on cancellation.
cart=req('POST','/cart/items',{'productId':product['id'],'quantity':3},buyer)
assert len(cart['items'])==1 and cart['items'][0]['quantity']==3
order=req('POST','/orders',{'shippingAddress':'Bat Trang','paymentMethod':'COD'},buyer,status=201)
assert req('GET','/products/'+product['id'])['stock']==97
req('PATCH','/orders/'+order['id']+'/cancel',{'reason':'Smoke'},buyer)
assert req('GET','/products/'+product['id'])['stock']==100
# A failed multi-product checkout rolls back earlier inventory writes and keeps cart items.
products=[req('POST','/products',{'name':'Rollback item','price':100,'stock':10,'images':[]},artisan,status=201) for _ in range(2)]
products.sort(key=lambda p:p['id'])
for p in products: req('POST','/cart/items',{'productId':p['id'],'quantity':1},buyer)
req('PUT','/products/'+products[1]['id'],{'stock':0},artisan)
req('POST','/orders',{'shippingAddress':'Test rollback'},buyer,status=409)
assert req('GET','/products/'+products[0]['id'])['stock']==10
assert len(req('GET','/cart',token=buyer)['items'])==2
req('DELETE','/cart',token=buyer)
assert req('GET','/cart',token=buyer)['items']==[]
# Category/news authorization and CRUD, invalid enum values, pagination bounds.
req('POST','/categories',{'name':'Denied category'},buyer,status=403)
category=req('POST','/categories',{'name':'Smoke category'},admin,status=201)
req('PUT','/categories/'+category['id'],{'name':'Updated category'},admin)
req('DELETE','/categories/'+category['id'],token=admin)
news_payload={'title':'Smoke news','content':'Community announcement','type':'policy'}
req('POST','/news',news_payload,buyer,status=403)
news=req('POST','/news',news_payload,admin,status=201)
req('PUT','/news/'+news['id'],{'title':'Updated news'},admin)
req('GET','/news/'+news['id'])
req('GET','/news?type=bad',status=400)
req('DELETE','/news/'+news['id'],token=admin)
for path in ('/products?page=0','/products?limit=101','/artisans?limit=0','/news?page=-1'):
    req('GET',path,status=400)
req('GET','/admin/quotes?status=bad',token=admin,status=400)
req('GET','/orders?status=bad',token=buyer,status=400)
req('PUT','/batches/'+batch['id'],{'status':'bad'},artisan,status=400)
req('POST','/auth/register/artisan',{'workshopName':'No password','ownerName':'Test','email':'missing'+suffix+'@test.vn','address':'Bat Trang'},status=400)
qr=req('GET','/qr/'+batch['id'])
http_calls+=1
with urllib.request.urlopen(qr['qrCodeUrl']) as r: assert r.read(8)==b'\x89PNG\r\n\x1a\n'
# A stolen product ID must not let a different workshop store uploads.
request=urllib.request.Request(BASE+'/api/v1/products/'+product['id']+'/images',data=body,headers={'Authorization':'Bearer '+second,'Content-Type':'multipart/form-data; boundary='+boundary},method='POST')
http_calls+=1
try:
    urllib.request.urlopen(request); raise AssertionError('Expected forbidden image upload')
except urllib.error.HTTPError as e: assert e.code==403
# Rotation invalidates old refresh credentials; password change invalidates refresh tokens.
rotated=req('POST','/auth/refresh-token',{'refreshToken':buyer_auth['refreshToken']})
req('POST','/auth/refresh-token',{'refreshToken':buyer_auth['refreshToken']},status=401)
req('PUT','/auth/change-password',{'oldPassword':'Password123!','newPassword':'UpdatedPassword123!'},buyer)
req('POST','/auth/refresh-token',{'refreshToken':rotated['refreshToken']},status=401)
req('POST','/auth/login',{'email':suffix+'@test.vn','password':'Password123!'},status=401)
req('POST','/auth/login',{'email':suffix+'@test.vn','password':'UpdatedPassword123!'})
# Blocking by generic user endpoint must also hide the workshop; approval restores consistency.
req('PATCH',f"/admin/users/{b['userId']}/block",{'reason':'Block via users'},admin)
req('GET','/auth/me',token=second,status=401)
req('GET','/artisans/'+b['id'],status=404)
req('PATCH',f"/admin/artisans/{b['id']}/approve",token=admin)
req('POST','/auth/login',{'email':'b'+suffix+'@test.vn','password':'Password123!'})
req('GET','/auth/me',token='invalid-token',status=401)
for origin,expected in [('https://untrusted.example',403),('http://localhost:3000',200)]:
    http_calls+=1
    request=urllib.request.Request(BASE+'/api/v1/health',headers={'Origin':origin})
    try:
        with urllib.request.urlopen(request) as r: actual=r.status
    except urllib.error.HTTPError as e: actual=e.code
    assert actual==expected,(origin,actual)
results.append('CORS: allowed localhost 200; untrusted origin 403')
req('PATCH',f'/admin/artisans/{artisan_id}/block',{'reason':'Smoke test'},admin)
req('GET','/auth/me',token=artisan,status=401)
print('\n'.join(results)); print('PASS:',http_calls,'HTTP calls with assertions; concurrent acceptance:',sorted(codes))
