package br.ufscar.pescd.service;

import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.OfferStudent;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.OfferStatus;
import br.ufscar.pescd.enums.UserRole;
import br.ufscar.pescd.repository.OfferRepository;
import br.ufscar.pescd.repository.OfferStudentRepository;
import br.ufscar.pescd.repository.StatusChangeLogRepository;
import br.ufscar.pescd.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecretaryOfferCsvBugFixTest {

    private OfferRepository offerRepository;
    private OfferStudentRepository offerStudentRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private StatusChangeLogRepository statusChangeLogRepository;
    private OfferApiMapper offerApiMapper;
    private SecretaryOfferService secretaryOfferService;

    @BeforeEach
    public void setUp() {
        offerRepository = mock(OfferRepository.class);
        offerStudentRepository = mock(OfferStudentRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        statusChangeLogRepository = mock(StatusChangeLogRepository.class);
        offerApiMapper = mock(OfferApiMapper.class);

        secretaryOfferService = new SecretaryOfferService(
                offerRepository,
                offerStudentRepository,
                userRepository,
                passwordEncoder,
                statusChangeLogRepository,
                offerApiMapper
        );
    }

    private Offer createActiveOffer(Long id) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setName("Algoritmo e Estrutura de Dados I");
        offer.setSemester("2026/1");
        offer.setStatus(OfferStatus.EM_ANDAMENTO);
        return offer;
    }

    @Test
    public void importStudentsFromCsvRejectsEmailBelongingToNonStudentUser() {
        Offer offer = createActiveOffer(1L);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        User professor = new User();
        professor.setId(10L);
        professor.setEmail("andre.endo@ufscar.br");
        professor.setRole(UserRole.PROFESSOR);
        when(userRepository.findByEmail("andre.endo@ufscar.br")).thenReturn(Optional.of(professor));

        String csv = "RA,NOME_COMPLETO,EMAIL\n123456,André Endo,andre.endo@ufscar.br\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "alunos.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            secretaryOfferService.importStudentsFromCsv(1L, file);
        });

        assertTrue(ex.getMessage().contains("não é aluno"));
        verify(offerStudentRepository, never()).save(any(OfferStudent.class));
    }

    @Test
    public void importStudentsFromCsvRejectsNewStudentWhenUsernameAlreadyExists() {
        Offer offer = createActiveOffer(1L);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(userRepository.findByEmail("conflito@ufscar.br")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("conflito@ufscar.br")).thenReturn(true);

        String csv = "RA,NOME_COMPLETO,EMAIL\n654321,Aluno Conflito,conflito@ufscar.br\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "alunos.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            secretaryOfferService.importStudentsFromCsv(1L, file);
        });

        assertTrue(ex.getMessage().contains("já existe um usuário com este e-mail"));
        verify(userRepository, never()).save(any(User.class));
        verify(offerStudentRepository, never()).save(any(OfferStudent.class));
    }

    @Test
    public void importStudentsFromCsvEnrollsValidNewAndExistingStudents() {
        Offer offer = createActiveOffer(1L);
        when(offerRepository.findById(1L)).thenReturn(Optional.of(offer));

        User existingStudent = new User();
        existingStudent.setId(20L);
        existingStudent.setEmail("existente@estudante.ufscar.br");
        existingStudent.setRole(UserRole.ALUNO);
        when(userRepository.findByEmail("existente@estudante.ufscar.br")).thenReturn(Optional.of(existingStudent));

        when(userRepository.findByEmail("novo@estudante.ufscar.br")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("novo@estudante.ufscar.br")).thenReturn(false);
        when(passwordEncoder.encode("111222")).thenReturn("encoded111222");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(offerStudentRepository.existsByOfferAndStudent(eq(offer), any(User.class))).thenReturn(false);

        String csv = "RA,NOME_COMPLETO,EMAIL\n"
                + "111222,Novo Aluno,novo@estudante.ufscar.br\n"
                + "333444,Aluno Existente,existente@estudante.ufscar.br\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "alunos.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        int enrolled = secretaryOfferService.importStudentsFromCsv(1L, file);

        assertEquals(2, enrolled);
        verify(offerStudentRepository, times(2)).save(any(OfferStudent.class));
    }
}
