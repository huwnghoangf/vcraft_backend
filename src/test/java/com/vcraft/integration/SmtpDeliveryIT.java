package com.vcraft.integration;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcraft.domain.exception.ServiceUnavailableException;
import com.vcraft.infrastructure.mail.SmtpPasswordResetDelivery;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Mailpit keeps these messages inside the test container; nothing is relayed externally. */
@Testcontainers
class SmtpDeliveryIT {
    @Container static final GenericContainer<?> inbox = new GenericContainer<>("axllent/mailpit:v1.27").withExposedPorts(1025,8025);
    @Test void smtpDeliversOneTimeLinkAndRequiresTlsWhenConfigured() throws Exception {
        String token="a".repeat(43);
        var mail = new SmtpPasswordResetDelivery(true,inbox.getHost(),inbox.getMappedPort(1025),"","",
                "noreply@vcraft.local","http://localhost:3000/reset-password",false);
        assertThat(mail.available()).isTrue();
        mail.send("buyer@example.com",token);
        var http=HttpClient.newHttpClient();var json=new ObjectMapper();
        String base="http://"+inbox.getHost()+":"+inbox.getMappedPort(8025);
        var response=http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/messages")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var messages=json.readTree(response.body()).get("messages");
        assertThat(messages.size()).isEqualTo(1);
        var detail=http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/message/"+messages.get(0).get("ID").asText())).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(json.readTree(detail.body()).get("Text").asText()).contains("token="+token,"15 phút");
        var tlsRequired=new SmtpPasswordResetDelivery(true,inbox.getHost(),inbox.getMappedPort(1025),"","",
                "noreply@vcraft.local","http://localhost:3000/reset-password",true);
        assertThatThrownBy(() -> tlsRequired.send("buyer@example.com",token)).isInstanceOf(ServiceUnavailableException.class);
    }
}
