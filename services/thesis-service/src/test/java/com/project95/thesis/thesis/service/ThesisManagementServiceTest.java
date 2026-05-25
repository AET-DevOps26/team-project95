package com.project95.thesis.thesis.service;

import com.project95.thesis.management.dto.ChairThesesReplacementRequest;
import com.project95.thesis.management.dto.ThesisProposalInput;
import com.project95.thesis.thesis.domain.Chair;
import com.project95.thesis.thesis.domain.ThesisProposal;
import com.project95.thesis.thesis.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThesisManagementServiceTest {

    @Mock private ThesisProposalRepository thesisRepository;
    @Mock private ChairRepository chairRepository;
    @Mock private TagRepository tagRepository;
    @Mock private AdvisorRepository advisorRepository;
    @Mock private ResearchAreaRepository researchAreaRepository;

    @InjectMocks
    private ThesisManagementService service;

    private Chair testChair;

    @BeforeEach
    void setUp() {
        testChair = new Chair();
        testChair.setId(1L);
    }

    @Test
    void replaceThesesInDatabase_Success() {
        // Arrange
        Long chairId = 1L;
        when(chairRepository.findById(chairId)).thenReturn(Optional.of(testChair));

        ThesisProposalInput input = new ThesisProposalInput();
        input.setTitle("AI in Medicine");
        input.setSourceUrl(URI.create("http://example.com/thesis"));
        input.setDegreeType(JsonNullable.of("MASTER"));

        ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();
        request.setTheses(List.of(input));

        // Return the items being saved
        when(thesisRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Act
        List<ThesisProposal> result = service.replaceThesesInDatabase(chairId, request);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("AI in Medicine");
        assertThat(result.get(0).getDegreeType()).isEqualTo("MASTER");
        
        verify(thesisRepository).deleteByChairId(chairId);
        verify(thesisRepository).flush();
        verify(thesisRepository).saveAll(anyList());
    }

    @Test
    void replaceThesesInDatabase_ThrowsIfChairNotFound() {
        // Arrange
        Long chairId = 99L;
        when(chairRepository.findById(chairId)).thenReturn(Optional.empty());
        ChairThesesReplacementRequest request = new ChairThesesReplacementRequest();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            service.replaceThesesInDatabase(chairId, request)
        );
        verify(thesisRepository, never()).deleteByChairId(any());
    }
}
