package br.ufscar.pescd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectReportFormDTO {

    @NotBlank(message = "{rejectReport.parecerRequired}")
    private String parecer;
}
