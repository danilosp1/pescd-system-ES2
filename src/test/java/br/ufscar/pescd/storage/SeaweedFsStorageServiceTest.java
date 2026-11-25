package br.ufscar.pescd.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SeaweedFsStorageServiceTest {

    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.ResponseSpec responseSpec;
    private SeaweedFsStorageService storageService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        restClient = mock(RestClient.class);
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(restClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any(MediaType.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        storageService = new SeaweedFsStorageService("http://localhost:9333/");
        ReflectionTestUtils.setField(storageService, "restClient", restClient);
    }

    private Object createAssignResponse(String fid, String url) throws Exception {
        Class<?> clazz = Class.forName("br.ufscar.pescd.storage.SeaweedFsStorageService$AssignResponse");
        Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(fid, url, url);
    }

    private Object createLookupResponse(String volumeId, List<String> urls) throws Exception {
        Class<?> locClass = Class.forName("br.ufscar.pescd.storage.SeaweedFsStorageService$Location");
        Constructor<?> locCtor = locClass.getDeclaredConstructor(String.class, String.class);
        locCtor.setAccessible(true);

        List<?> locations = urls == null ? null : urls.stream().map(u -> {
            try {
                return locCtor.newInstance(u, u);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        Class<?> lookupClass = Class.forName("br.ufscar.pescd.storage.SeaweedFsStorageService$LookupResponse");
        Constructor<?> lookupCtor = lookupClass.getDeclaredConstructor(String.class, List.class);
        lookupCtor.setAccessible(true);
        return lookupCtor.newInstance(volumeId, locations);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void storeUploadsFileWithProvidedFileNameAndReturnsFid() throws Exception {
        SeaweedFsStorageService serviceNoSlash = new SeaweedFsStorageService("http://localhost:9333");
        ReflectionTestUtils.setField(serviceNoSlash, "restClient", restClient);

        Object assign = createAssignResponse("3,01637037d6", "localhost:8081");
        when(responseSpec.body(any(Class.class))).thenReturn(assign);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        byte[] content = "conteudo-pdf".getBytes(StandardCharsets.UTF_8);
        String fid = serviceNoSlash.store(content, "plano.pdf", "application/pdf");

        assertEquals("3,01637037d6", fid);
        ArgumentCaptor<MultiValueMap> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec).body(bodyCaptor.capture());
        ByteArrayResource resource = (ByteArrayResource) bodyCaptor.getValue().getFirst("file");
        assertEquals("plano.pdf", resource.getFilename());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void storeUsesFidAsFileNameWhenFileNameIsNullOrBlank() throws Exception {
        Object assign = createAssignResponse("5,abc", "localhost:8081");
        when(responseSpec.body(any(Class.class))).thenReturn(assign);

        byte[] content = "pdf".getBytes(StandardCharsets.UTF_8);
        storageService.store(content, null, "application/pdf");
        storageService.store(content, "   ", "application/pdf");

        ArgumentCaptor<MultiValueMap> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec, times(2)).body(bodyCaptor.capture());
        ByteArrayResource resource = (ByteArrayResource) bodyCaptor.getValue().getFirst("file");
        assertEquals("5,abc", resource.getFilename());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void storeThrowsWhenAssignResponseIsInvalid() throws Exception {
        when(responseSpec.body(any(Class.class))).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> {
            storageService.store(new byte[]{1}, "a.pdf", "application/pdf");
        });

        Object assignNullFid = createAssignResponse(null, "localhost:8081");
        when(responseSpec.body(any(Class.class))).thenReturn(assignNullFid);
        assertThrows(IllegalStateException.class, () -> {
            storageService.store(new byte[]{1}, "a.pdf", "application/pdf");
        });

        Object assignNullUrl = createAssignResponse("3,01", null);
        when(responseSpec.body(any(Class.class))).thenReturn(assignNullUrl);
        assertThrows(IllegalStateException.class, () -> {
            storageService.store(new byte[]{1}, "a.pdf", "application/pdf");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readReturnsFileBytesWhenVolumeIsFound() throws Exception {
        Object lookup = createLookupResponse("3", List.of("localhost:8081"));
        byte[] expected = "dados".getBytes(StandardCharsets.UTF_8);
        when(responseSpec.body(any(Class.class))).thenReturn(lookup, expected);

        byte[] actual = storageService.read("3,01637037d6");

        assertArrayEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void readHandlesFidWithoutCommaAndThrowsWhenVolumeNotFound() throws Exception {
        when(responseSpec.body(any(Class.class))).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> {
            storageService.read("3");
        });

        Object lookupNullLocations = createLookupResponse("3", null);
        when(responseSpec.body(any(Class.class))).thenReturn(lookupNullLocations);
        assertThrows(IllegalStateException.class, () -> {
            storageService.read("3,01");
        });

        Object lookupEmptyLocations = createLookupResponse("3", Collections.emptyList());
        when(responseSpec.body(any(Class.class))).thenReturn(lookupEmptyLocations);
        assertThrows(IllegalStateException.class, () -> {
            storageService.read("3,01");
        });
    }

    @Test
    public void readThrowsWhenFidIsNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> {
            storageService.read(null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            storageService.read("   ");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deleteIgnoresNullOrBlankFidAndDeletesValidFid() throws Exception {
        storageService.delete(null);
        storageService.delete("  ");
        verify(restClient, never()).delete();

        Object lookup = createLookupResponse("3", List.of("localhost:8081"));
        when(responseSpec.body(any(Class.class))).thenReturn(lookup);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        storageService.delete("3,01637037d6");

        verify(restClient).delete();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deleteSwallowsRuntimeExceptionOnFailure() {
        when(responseSpec.body(any(Class.class))).thenThrow(new RuntimeException("Erro de rede"));

        assertDoesNotThrow(() -> {
            storageService.delete("3,01637037d6");
        });
    }
}
