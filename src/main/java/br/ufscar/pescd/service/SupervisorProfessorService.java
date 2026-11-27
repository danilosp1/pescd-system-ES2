package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.ApprovePlanFormDTO;
import br.ufscar.pescd.dto.ApproveReportFormDTO;
import br.ufscar.pescd.dto.OfferDetailsResponse;
import br.ufscar.pescd.dto.OfferStudentDetailsResponse;
import br.ufscar.pescd.dto.OfferStudentSummaryResponse;
import br.ufscar.pescd.dto.RejectPlanFormDTO;
import br.ufscar.pescd.dto.RejectReportFormDTO;
import br.ufscar.pescd.dto.ReportDetailsResponse;
import br.ufscar.pescd.dto.SupervisorDashboardDTO;
import br.ufscar.pescd.dto.WorkPlanDetailsResponse;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.enums.OfferStatus;
import br.ufscar.pescd.enums.StudentOfferStatus;
import br.ufscar.pescd.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SupervisorProfessorService {

    private final OfferStudentRepository offerStudentRepository;
    private final WorkPlanRepository workPlanRepository;
    private final ReportRepository reportRepository;
    private final StatusChangeLogRepository statusChangeLogRepository;
    private final OfferApiMapper offerApiMapper;

    public SupervisorProfessorService(
            OfferStudentRepository offerStudentRepository,
            WorkPlanRepository workPlanRepository,
            ReportRepository reportRepository,
            StatusChangeLogRepository statusChangeLogRepository,
            OfferApiMapper offerApiMapper
    ) {
        this.offerStudentRepository = offerStudentRepository;
        this.workPlanRepository = workPlanRepository;
        this.reportRepository = reportRepository;
        this.statusChangeLogRepository = statusChangeLogRepository;
        this.offerApiMapper = offerApiMapper;
    }

    // -------------------------------------------------------------------------
    // PS.01 — Dashboard
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SupervisorDashboardDTO> getDashboard(User professor) {

        List<OfferStudent> supervised =
                offerStudentRepository.findBySupervisor(professor);

        Map<Offer, List<OfferStudent>> byOffer = supervised.stream()
                .collect(Collectors.groupingBy(OfferStudent::getOffer));

        return byOffer.entrySet().stream()
                .map(entry -> new SupervisorDashboardDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(
                        dto -> dto.getOffer().getSemester(),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfferDetailsResponse> getDashboardForApi(User professor) {
        return getDashboard(professor).stream()
                .map(item -> offerApiMapper.toOfferDetails(item.getOffer(), item.getSupervisedStudents()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferStudentDetailsResponse getEnrollmentDetailsForApi(Long offerId, Long studentId, User professor) {
        OfferStudent os = getEnrollmentForSupervisor(offerId, studentId, professor);
        return offerApiMapper.toOfferStudentDetails(os);
    }

    // -------------------------------------------------------------------------
    // PS.02 — Aprovar Plano de Trabalho
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OfferStudent getEnrollmentForSupervisor(Long offerId, Long studentId, User professor) {
        return requireSupervisorEnrollment(offerStudentRepository.findByOffer_IdAndStudent_Id(offerId, studentId),
                professor);
    }

    private OfferStudent requireSupervisorEnrollment(Optional<OfferStudent> result, User professor) {
        OfferStudent os = result
                .orElseThrow(() -> new IllegalArgumentException("Inscrição não encontrada."));

        if (!professor.equals(os.getSupervisor())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Você não é o supervisor deste aluno nesta oferta.");
        }

        return os;
    }

    @Transactional(readOnly = true)
    public WorkPlan getWorkPlan(OfferStudent offerStudent) {
        return workPlanRepository.findByOfferStudent(offerStudent)
                .orElseThrow(() -> new IllegalArgumentException("Plano de trabalho não encontrado."));
    }

    @Transactional(readOnly = true)
    public WorkPlanDetailsResponse getWorkPlanDetailsForApi(Long offerId, Long studentId, User professor) {
        OfferStudent os = getEnrollmentForSupervisor(offerId, studentId, professor);
        return offerApiMapper.toWorkPlanDetails(getWorkPlan(os));
    }

    @Transactional
    public void approvePlan(Long offerId, Long studentId, User professor, String parecer) {

        OfferStudent os = getEnrollmentForSupervisor(offerId, studentId, professor);

        if (os.getStatus() != StudentOfferStatus.PLANO_ENVIADO) {
            throw new IllegalArgumentException(
                    "Ação indisponível: o status atual é \""
                    + os.getStatus().getDisplayName() + "\".");
        }

        WorkPlan plan = getWorkPlan(os);
        plan.setSupervisorParecer(parecer);
        plan.setSupervisorApprovedAt(LocalDateTime.now());
        plan.setSupervisorRejectedAt(null);
        workPlanRepository.save(plan);

        changeStatus(os, StudentOfferStatus.PLANO_APROVADO, "Plano de trabalho aprovado pelo professor supervisor");
    }

    @Transactional
    public OfferStudentSummaryResponse approvePlanForApi(
            Long offerId,
            Long studentId,
            User professor,
            ApprovePlanFormDTO form
    ) {
        approvePlan(offerId, studentId, professor, form.getParecer());
        return offerApiMapper.toOfferStudentSummary(getEnrollmentForSupervisor(offerId, studentId, professor));
    }

    @Transactional
    public void rejectPlan(Long offerId, Long studentId, User professor, String parecer) {

        OfferStudent os = getEnrollmentForSupervisor(offerId, studentId, professor);

        if (os.getStatus() != StudentOfferStatus.PLANO_ENVIADO) {
            throw new IllegalArgumentException(
                    "Ação indisponível: o status atual é \""
                    + os.getStatus().getDisplayName() + "\".");
        }

        WorkPlan plan = getWorkPlan(os);
        plan.setSupervisorParecer(parecer);
        plan.setSupervisorApprovedAt(null);
        plan.setSupervisorRejectedAt(LocalDateTime.now());
        workPlanRepository.save(plan);

        changeStatus(os, StudentOfferStatus.PLANO_REPROVADO,
                "Plano de trabalho reprovado pelo professor supervisor");
    }

    @Transactional
    public OfferStudentSummaryResponse rejectPlanForApi(
            Long offerId,
            Long studentId,
            User professor,
            RejectPlanFormDTO form
    ) {
        rejectPlan(offerId, studentId, professor, form.getParecer());
        return offerApiMapper.toOfferStudentSummary(getEnrollmentForSupervisor(offerId, studentId, professor));
    }

    // -------------------------------------------------------------------------
    // PS.03 — Aprovar Relatório
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Report getReport(OfferStudent offerStudent) {
        return reportRepository.findByOfferStudent(offerStudent)
                .orElseThrow(() -> new IllegalArgumentException("Relatório não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<StatusChangeLog> getStatusLogs(OfferStudent offerStudent) {
        return statusChangeLogRepository.findByOfferStudentOrderByChangedAtAsc(offerStudent);
    }

    @Transactional(readOnly = true)
    public ReportDetailsResponse getReportDetailsForApi(Long offerId, Long studentId, User professor) {
        OfferStudent os = getEnrollmentForSupervisor(offerId, studentId, professor);
        return offerApiMapper.toReportDetails(getReport(os));
    }

    @Transactional
    public void approveReport(Long offerId, Long studentId, User professor, ApproveReportFormDTO form) {
        OfferStudent os = getPendingReportEnrollment(offerId, studentId, professor);

        Report report = getReport(os);
        report.setSupervisorParecer(form.getParecer());
        report.setSupervisorFrequencia(form.getFrequencia());
        report.setSupervisorNotaSugestao(form.getNotaSugestao());
        report.setSupervisorApprovedAt(LocalDateTime.now());
        reportRepository.save(report);

        changeStatus(os, StudentOfferStatus.RELATORIO_APROVADO_SUPERVISOR,
                "Relatório aprovado pelo professor supervisor");
    }

    @Transactional
    public OfferStudentSummaryResponse approveReportForApi(
            Long offerId,
            Long studentId,
            User professor,
            ApproveReportFormDTO form
    ) {
        approveReport(offerId, studentId, professor, form);
        return offerApiMapper.toOfferStudentSummary(getEnrollmentForSupervisor(offerId, studentId, professor));
    }

    @Transactional
    public OfferStudentSummaryResponse rejectReport(
            Long offerId, Long studentId, User professor, RejectReportFormDTO form
    ) {
        OfferStudent os = getPendingReportEnrollment(offerId, studentId, professor);
        if (os.getOffer().getStatus() != OfferStatus.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Esta ação só é permitida em ofertas em andamento.");
        }

        Report report = getReport(os);
        report.setSupervisorParecer(form.getParecer().strip());
        reportRepository.save(report);
        changeStatus(os, StudentOfferStatus.RELATORIO_REPROVADO,
                "Relatório reprovado pelo professor supervisor");
        return offerApiMapper.toOfferStudentSummary(os);
    }

    private OfferStudent getPendingReportEnrollment(Long offerId, Long studentId, User professor) {
        OfferStudent os = requireSupervisorEnrollment(
                offerStudentRepository.findForUpdateByOffer_IdAndStudent_Id(offerId, studentId), professor);
        if (os.getStatus() != StudentOfferStatus.RELATORIO_ENVIADO) {
            throw new IllegalArgumentException(
                    "Ação indisponível: o status atual é \"" + os.getStatus().getDisplayName() + "\".");
        }
        return os;
    }

    // -------------------------------------------------------------------------
    // Utilitário — log de status
    // -------------------------------------------------------------------------

    private void changeStatus(OfferStudent os, StudentOfferStatus newStatus, String description) {
        StatusChangeLog log = new StatusChangeLog();
        log.setOfferStudent(os);
        log.setPreviousStatus(os.getStatus());
        log.setNewStatus(newStatus);
        log.setDescription(description);
        statusChangeLogRepository.save(log);

        os.setStatus(newStatus);
        offerStudentRepository.save(os);
    }
}
