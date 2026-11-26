package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.OfferForm;
import br.ufscar.pescd.dto.SecretaryCloseOfferPreviewResponse;
import br.ufscar.pescd.dto.StudentForm;
import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.OfferStudent;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.OfferStatus;
import br.ufscar.pescd.enums.StudentOfferStatus;
import br.ufscar.pescd.enums.UserRole;
import br.ufscar.pescd.repository.OfferRepository;
import br.ufscar.pescd.repository.OfferStudentRepository;
import br.ufscar.pescd.repository.StatusChangeLogRepository;
import br.ufscar.pescd.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SecretaryOfferServiceTest {

    private OfferRepository offerRepository;
    private OfferStudentRepository offerStudentRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private StatusChangeLogRepository statusChangeLogRepository;
    private OfferApiMapper offerApiMapper;
    private OfferClosureService offerClosureService;
    private SecretaryOfferService service;

    @BeforeEach
    public void setUp() {
        offerRepository = mock(OfferRepository.class);
        offerStudentRepository = mock(OfferStudentRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        statusChangeLogRepository = mock(StatusChangeLogRepository.class);
        offerApiMapper = mock(OfferApiMapper.class);
        offerClosureService = mock(OfferClosureService.class);
        service = new SecretaryOfferService(
                offerRepository,
                offerStudentRepository,
                userRepository,
                passwordEncoder,
                statusChangeLogRepository,
                offerApiMapper,
                offerClosureService
        );
    }

    private Offer createOffer(Long id, OfferStatus status) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setName("Oferta");
        offer.setSemester("2026/1");
        offer.setStartDate(LocalDate.of(2026, 3, 1));
        offer.setEndDate(LocalDate.of(2026, 7, 1));
        offer.setStatus(status);
        return offer;
    }

    private User createUser(Long id, String username, String email, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setFullName("Usuário " + username);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        user.setPassword("encoded");
        return user;
    }

    private OfferForm createOfferForm(String name) {
        OfferForm form = new OfferForm();
        form.setName(name);
        form.setSemester("2026/1");
        form.setStartDate(LocalDate.of(2026, 3, 1));
        form.setEndDate(LocalDate.of(2026, 7, 1));
        form.setResponsibleProfessorId(10L);
        return form;
    }

    private StudentForm createStudentForm() {
        StudentForm form = new StudentForm();
        form.setFullName("Aluno Novo");
        form.setEmail("aluno@ufscar.br");
        form.setRa("123456");
        return form;
    }

    @Test
    public void listOffersDelegatesToRepository() {
        List<Offer> offers = List.of(createOffer(1L, OfferStatus.EM_ANDAMENTO));
        when(offerRepository.findAllByOrderBySemesterDesc()).thenReturn(offers);

        assertEquals(offers, service.listOffers());
    }

    @Test
    public void listOffersForApiMapsOffers() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        when(offerRepository.findAllByOrderBySemesterDesc()).thenReturn(List.of(offer));
        when(offerApiMapper.toOfferSummary(offer)).thenReturn(null);

        assertEquals(1, service.listOffersForApi().size());
    }

    @Test
    public void getOfferThrowsWhenNotFound() {
        when(offerRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.getOffer(1L));
        assertEquals("Oferta não encontrada.", ex.getMessage());
    }

    @Test
    public void getOfferStudentsUsesRepository() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        List<OfferStudent> students = List.of(new OfferStudent());
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerStudentRepository.findByOffer(offer)).thenReturn(students);

        assertEquals(students, service.getOfferStudents(1L));
    }

    @Test
    public void getOfferDetailsForApiUsesMapper() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerStudentRepository.findByOffer(offer)).thenReturn(List.of());

        assertNull(service.getOfferDetailsForApi(1L));
        verify(offerApiMapper).toOfferDetails(offer, List.of());
    }

    @Test
    public void getStudentDetailsForApiRejectsStudentFromAnotherOffer() {
        Offer requestedOffer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        Offer otherOffer = createOffer(2L, OfferStatus.EM_ANDAMENTO);
        OfferStudent student = new OfferStudent();
        student.setOffer(otherOffer);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(requestedOffer));
        when(offerStudentRepository.findById(5L)).thenReturn(Optional.of(student));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getStudentDetailsForApi(1L, 5L));
        assertEquals("O aluno informado não pertence a esta oferta.", ex.getMessage());
    }

    @Test
    public void getStudentDetailsForApiReturnsMappedDetails() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        OfferStudent student = new OfferStudent();
        student.setId(5L);
        student.setOffer(offer);

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(offerStudentRepository.findById(5L)).thenReturn(Optional.of(student));

        assertNull(service.getStudentDetailsForApi(1L, 5L));
        verify(offerApiMapper).toOfferStudentDetails(student);
    }

    @Test
    public void getCloseOfferPreviewDelegatesToClosureService() {
        SecretaryCloseOfferPreviewResponse expected =
                new SecretaryCloseOfferPreviewResponse(null, true, "Instruções");
        when(offerClosureService.getCloseOfferPreviewForApi(1L)).thenReturn(expected);

        assertSame(expected, service.getCloseOfferPreviewForApi(1L));
    }

    @Test
    public void getStudentLogsUsesStatusLogRepository() {
        OfferStudent student = new OfferStudent();
        when(offerStudentRepository.findById(5L)).thenReturn(Optional.of(student));
        when(statusChangeLogRepository.findByOfferStudentOrderByChangedAtAsc(student)).thenReturn(List.of());

        assertTrue(service.getStudentLogs(5L).isEmpty());
    }

    @Test
    public void listProfessorsForApiMapsProfessors() {
        User professor = createUser(10L, "prof", "prof@ufscar.br", UserRole.PROFESSOR);
        when(userRepository.findByRole(UserRole.PROFESSOR)).thenReturn(List.of(professor));
        when(offerApiMapper.toUserSummary(professor)).thenReturn(null);

        assertEquals(1, service.listProfessorsForApi().size());
    }

    @Test
    public void createOfferForApiRejectsInvalidDateRange() {
        OfferForm form = createOfferForm("Oferta");
        form.setEndDate(form.getStartDate());

        assertThrows(IllegalArgumentException.class,
                () -> service.createOfferForApi(form, "secretario"));
        verify(offerRepository, never()).save(any());
    }

    @Test
    public void createOfferUsesDefaultNameWhenNameIsBlank() {
        OfferForm form = createOfferForm("   ");
        User professor = createUser(10L, "prof", "prof@ufscar.br", UserRole.PROFESSOR);
        User creator = createUser(20L, "secretario", "sec@ufscar.br", UserRole.SECRETARIO);

        when(userRepository.findById(10L)).thenReturn(Optional.of(professor));
        when(userRepository.findByUsername("secretario")).thenReturn(Optional.of(creator));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Offer saved = service.createOffer(form, "secretario");

        assertEquals("Oferta PESCD 2026/1", saved.getName());
        assertEquals(OfferStatus.EM_ANDAMENTO, saved.getStatus());
        assertSame(professor, saved.getResponsibleProfessor());
        assertSame(creator, saved.getCreatedBy());
    }

    @Test
    public void createOfferTrimsProvidedName() {
        OfferForm form = createOfferForm("  Oferta Customizada  ");
        User professor = createUser(10L, "prof", "prof@ufscar.br", UserRole.PROFESSOR);
        User creator = createUser(20L, "secretario", "sec@ufscar.br", UserRole.SECRETARIO);

        when(userRepository.findById(10L)).thenReturn(Optional.of(professor));
        when(userRepository.findByUsername("secretario")).thenReturn(Optional.of(creator));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("Oferta Customizada", service.createOffer(form, "secretario").getName());
    }

    @Test
    public void createOfferRejectsNonProfessorResponsible() {
        OfferForm form = createOfferForm("Oferta");
        User aluno = createUser(10L, "aluno", "aluno@ufscar.br", UserRole.ALUNO);
        when(userRepository.findById(10L)).thenReturn(Optional.of(aluno));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createOffer(form, "secretario"));
        assertEquals("Professor responsável inválido.", ex.getMessage());
    }

    @Test
    public void createOfferThrowsWhenCreatorDoesNotExist() {
        OfferForm form = createOfferForm("Oferta");
        User professor = createUser(10L, "prof", "prof@ufscar.br", UserRole.PROFESSOR);
        when(userRepository.findById(10L)).thenReturn(Optional.of(professor));
        when(userRepository.findByUsername("ausente")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.createOffer(form, "ausente"));
    }

    @Test
    public void addStudentCreatesNewUserAndEnrolls() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        StudentForm form = createStudentForm();

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByEmail(form.getEmail())).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(form.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(form.getRa())).thenReturn("encoded-ra");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(offerStudentRepository.existsByOfferAndStudent(any(), any())).thenReturn(false);

        service.addStudent(1L, form);

        verify(passwordEncoder).encode("123456");
        verify(userRepository).save(any(User.class));
        verify(offerStudentRepository).save(any(OfferStudent.class));
    }

    @Test
    public void addStudentReusesExistingAluno() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        User aluno = createUser(30L, "aluno", "aluno@ufscar.br", UserRole.ALUNO);
        StudentForm form = createStudentForm();

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByEmail(form.getEmail())).thenReturn(Optional.of(aluno));
        when(offerStudentRepository.existsByOfferAndStudent(offer, aluno)).thenReturn(false);

        service.addStudent(1L, form);

        verify(userRepository, never()).save(any());
        verify(offerStudentRepository).save(any(OfferStudent.class));
    }

    @Test
    public void addStudentRejectsExistingNonAlunoUser() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        User professor = createUser(30L, "prof", "prof@ufscar.br", UserRole.PROFESSOR);
        StudentForm form = createStudentForm();
        form.setEmail("prof@ufscar.br");

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByEmail(form.getEmail())).thenReturn(Optional.of(professor));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addStudent(1L, form));
        assertEquals("O e-mail informado pertence a um usuário que não é aluno.", ex.getMessage());
    }

    @Test
    public void addStudentRejectsDuplicateEnrollment() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        User aluno = createUser(30L, "aluno", "aluno@ufscar.br", UserRole.ALUNO);
        StudentForm form = createStudentForm();

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByEmail(form.getEmail())).thenReturn(Optional.of(aluno));
        when(offerStudentRepository.existsByOfferAndStudent(offer, aluno)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addStudent(1L, form));
        assertEquals("Este aluno já está inscrito nesta oferta.", ex.getMessage());
    }

    @Test
    public void addStudentRejectsUsernameConflict() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        StudentForm form = createStudentForm();

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByEmail(form.getEmail())).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(form.getEmail())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addStudent(1L, form));
        assertEquals("Já existe um usuário com este e-mail.", ex.getMessage());
    }

    @Test
    public void addStudentRejectsClosedOffer() {
        Offer offer = createOffer(1L, OfferStatus.CONCLUIDA);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        assertThrows(IllegalArgumentException.class,
                () -> service.addStudent(1L, createStudentForm()));
    }

    @Test
    public void importStudentsFromCsvImportsNewAndExistingStudents() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        User existing = createUser(30L, "existente", "existente@ufscar.br", UserRole.ALUNO);
        String csv = "RA,NOME_COMPLETO,EMAIL\n"
                + "123,Aluno Novo,novo@ufscar.br\n"
                + "456,Aluno Existente,existente@ufscar.br\n";
        MockMultipartFile file = new MockMultipartFile("file", "alunos.csv", "text/csv", csv.getBytes());

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByEmail("novo@ufscar.br")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existente@ufscar.br")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("123")).thenReturn("encoded");
        when(offerStudentRepository.existsByOfferAndStudent(any(), any())).thenReturn(false);

        int enrolled = service.importStudentsFromCsv(1L, file);

        assertEquals(2, enrolled);
        verify(userRepository, times(1)).save(any(User.class));
        verify(offerStudentRepository, times(2)).save(any(OfferStudent.class));
    }

    @Test
    public void importStudentsFromCsvSkipsBlankLinesAndHeaderOnlyReturnsZero() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        MockMultipartFile headerOnly = new MockMultipartFile(
                "file", "alunos.csv", "text/csv", "RA,NOME_COMPLETO,EMAIL\n".getBytes());

        assertEquals(0, service.importStudentsFromCsv(1L, headerOnly));
    }

    @Test
    public void importStudentsFromCsvRejectsEmptyFile() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        MockMultipartFile empty = new MockMultipartFile("file", "alunos.csv", "text/csv", new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importStudentsFromCsv(1L, empty));
        assertEquals("Selecione um arquivo CSV.", ex.getMessage());
    }

    @Test
    public void importStudentsFromCsvSkipsBlankLines() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        String csv = "RA,NOME_COMPLETO,EMAIL\n\n123,Aluno Novo,novo@ufscar.br\n";
        MockMultipartFile file = new MockMultipartFile("file", "alunos.csv", "text/csv", csv.getBytes());

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByEmail("novo@ufscar.br")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("123")).thenReturn("encoded");
        when(offerStudentRepository.existsByOfferAndStudent(any(), any())).thenReturn(false);

        assertEquals(1, service.importStudentsFromCsv(1L, file));
    }

    @Test
    public void importStudentsFromCsvRejectsInvalidColumns() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        MockMultipartFile file = new MockMultipartFile(
                "file", "alunos.csv", "text/csv",
                "RA,NOME_COMPLETO,EMAIL\n123,Aluno\n".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importStudentsFromCsv(1L, file));
        assertTrue(ex.getMessage().contains("Linha 2 inválida"));
    }

    @Test
    public void importStudentsFromCsvRejectsEmptyFields() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        MockMultipartFile file = new MockMultipartFile(
                "file", "alunos.csv", "text/csv",
                "RA,NOME_COMPLETO,EMAIL\n123,,aluno@ufscar.br\n".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importStudentsFromCsv(1L, file));
        assertTrue(ex.getMessage().contains("campos vazios"));
    }

    @Test
    public void importStudentsFromCsvWrapsIoException() throws Exception {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        MultipartFile file = mock(MultipartFile.class);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("falha"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importStudentsFromCsv(1L, file));
        assertEquals("Não foi possível ler o arquivo CSV.", ex.getMessage());
    }

    @Test
    public void closeOfferDelegatesToClosureService() {
        service.closeOffer(1L);

        verify(offerClosureService).closeOffer(1L);
    }

    @Test
    public void closeOfferWithUsernameDelegatesToClosureService() {
        Offer offer = createOffer(1L, OfferStatus.CONCLUIDA);
        when(offerClosureService.closeOffer(1L, "secretario")).thenReturn(offer);

        assertSame(offer, service.closeOffer(1L, "secretario"));
    }

    @Test
    public void closeOfferForApiDelegatesToClosureService() {
        when(offerClosureService.closeOfferForApi(1L, "secretario")).thenReturn(null);

        assertNull(service.closeOfferForApi(1L, "secretario"));
        verify(offerClosureService).closeOfferForApi(1L, "secretario");
    }

    @Test
    public void getOfferStudentThrowsWhenNotFound() {
        when(offerStudentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getOfferStudent(5L));
    }

    @Test
    public void addStudentForApiReturnsUpdatedOfferDetails() {
        Offer offer = createOffer(1L, OfferStatus.EM_ANDAMENTO);
        User aluno = createUser(30L, "aluno", "aluno@ufscar.br", UserRole.ALUNO);
        StudentForm form = createStudentForm();

        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByEmail(form.getEmail())).thenReturn(Optional.of(aluno));
        when(offerStudentRepository.existsByOfferAndStudent(offer, aluno)).thenReturn(false);

        assertNull(service.addStudentForApi(1L, form));
        verify(offerApiMapper).toOfferDetails(offer, List.of());
    }
}
