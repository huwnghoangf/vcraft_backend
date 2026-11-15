package com.vcraft.api;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import com.vcraft.domain.exception.InvalidInputException;
import com.vcraft.domain.service.ImageStorageService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
class ImageUploadSupportTest {
    @Test void removesEarlierFilesWhenLaterImageFails() {
        var storage = mock(ImageStorageService.class);
        when(storage.storeImage(any(), any(), any())).thenReturn("http://localhost/uploads/one.jpg")
                .thenThrow(new InvalidInputException("bad image"));
        var file = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
        assertThatThrownBy(() -> ImageUploadSupport.storeAll(storage, List.of(file, file), "products"))
                .isInstanceOf(InvalidInputException.class);
        verify(storage).deleteImage("http://localhost/uploads/one.jpg");
    }
}
