package br.ufscar.pescd.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class SeaweedFsStorageService {

    private final RestClient restClient;
    private final String masterUrl;

    @Autowired
    public SeaweedFsStorageService(
            @Value("${seaweedfs.master-url:http://localhost:9333}") String masterUrl
    ) {
        this(masterUrl, RestClient.create());
    }

    public SeaweedFsStorageService(String masterUrl, RestClient restClient) {
        this.masterUrl = normalizeMasterUrl(masterUrl);
        this.restClient = restClient;
    }

    public String store(byte[] content, String fileName, String contentType) {
        AssignResponse assign = requestFileAssignment();
        String fid = assign.fid();
        MultiValueMap<String, Object> body = buildMultipartBody(content, fileName, fid);

        restClient.post()
                .uri(buildVolumeFileUri(assign.url(), fid))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        return fid;
    }

    public byte[] read(String fid) {
        if (fid == null || fid.isBlank()) {
            throw new IllegalArgumentException("Identificador de arquivo (fid) ausente.");
        }
        String volumeUrl = lookupVolumeUrl(fid);
        return restClient.get()
                .uri(buildVolumeFileUri(volumeUrl, fid))
                .retrieve()
                .body(byte[].class);
    }

    public void delete(String fid) {
        if (fid == null || fid.isBlank()) {
            return;
        }
        try {
            String volumeUrl = lookupVolumeUrl(fid);
            restClient.delete()
                    .uri(buildVolumeFileUri(volumeUrl, fid))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ignored) {
        }
    }

    private String normalizeMasterUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private AssignResponse requestFileAssignment() {
        AssignResponse assign = restClient.get()
                .uri(masterUrl + "/dir/assign")
                .retrieve()
                .body(AssignResponse.class);

        if (assign == null || assign.fid() == null || assign.url() == null) {
            throw new IllegalStateException("SeaweedFS não retornou um fid válido em /dir/assign.");
        }
        return assign;
    }

    private MultiValueMap<String, Object> buildMultipartBody(byte[] content, String fileName, String fid) {
        String safeName = (fileName != null && !fileName.isBlank()) ? fileName : fid;
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return safeName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        return body;
    }

    private String lookupVolumeUrl(String fid) {
        String volumeId = extractVolumeId(fid);
        LookupResponse lookup = restClient.get()
                .uri(masterUrl + "/dir/lookup?volumeId=" + volumeId)
                .retrieve()
                .body(LookupResponse.class);

        if (lookup == null || lookup.locations() == null || lookup.locations().isEmpty()) {
            throw new IllegalStateException("SeaweedFS não localizou o volume do arquivo " + fid + ".");
        }
        return lookup.locations().get(0).url();
    }

    private String extractVolumeId(String fid) {
        return fid.contains(",") ? fid.substring(0, fid.indexOf(',')) : fid;
    }

    private String buildVolumeFileUri(String volumeUrl, String fid) {
        return "http://" + volumeUrl + "/" + fid;
    }

    record AssignResponse(String fid, String url, String publicUrl) {
    }

    record LookupResponse(String volumeId, List<Location> locations) {
    }

    record Location(String url, String publicUrl) {
    }
}
