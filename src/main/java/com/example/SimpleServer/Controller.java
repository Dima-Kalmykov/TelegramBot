package com.example.SimpleServer;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


@RestController
public class Controller {

    @GetMapping(value = "/getTestTiff", produces = "image/tiff")
    public ResponseEntity<ByteArrayResource> getTiffImage() throws IOException {
        var bytes = Files.readAllBytes(Paths.get("C:/temp/naip/classified.tif"));
        ByteArrayResource outputData = new ByteArrayResource(bytes);

        return ResponseEntity
                .ok()
                .contentLength(outputData.contentLength())
                .contentType(MediaType.valueOf("image/tiff"))
                .body(outputData);
    }
}
