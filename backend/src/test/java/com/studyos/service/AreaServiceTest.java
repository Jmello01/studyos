package com.studyos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.entity.Area;
import com.studyos.exception.ConflictException;
import com.studyos.exception.ResourceNotFoundException;
import com.studyos.mapper.AreaMapper;
import com.studyos.repository.AreaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock
    private AreaRepository repository;

    private AreaService service;

    @BeforeEach
    void setUp() {
        service = new AreaService(repository, new AreaMapper());
    }

    private AreaRequest request(String name, Integer position) {
        return new AreaRequest(name, null, null, null, position);
    }

    @Test
    void criaAreaNoFimDaListaQuandoNaoInformaPosicao() {
        when(repository.existsByNameIgnoreCase("Estudos")).thenReturn(false);
        when(repository.findMaxPosition()).thenReturn(9);
        when(repository.save(any(Area.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AreaResponse response = service.create(request("Estudos", null));

        assertThat(response.name()).isEqualTo("Estudos");
        assertThat(response.position()).isEqualTo(10);
        assertThat(response.archived()).isFalse();
    }

    @Test
    void criaAreaComPosicaoInformada() {
        when(repository.existsByNameIgnoreCase("Estudos")).thenReturn(false);
        when(repository.save(any(Area.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AreaResponse response = service.create(request("Estudos", 3));

        assertThat(response.position()).isEqualTo(3);
        verify(repository, never()).findMaxPosition();
    }

    @Test
    void criarComNomeDuplicadoLancaConflito() {
        when(repository.existsByNameIgnoreCase("CLOUD")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("CLOUD", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CLOUD");

        verify(repository, never()).save(any(Area.class));
    }

    @Test
    void requestRemoveEspacosDoNome() {
        assertThat(request("  Estudos  ", null).name()).isEqualTo("Estudos");
    }

    @Test
    void atualizaCamposDaArea() {
        Area existente = new Area();
        existente.setName("Antigo");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByNameIgnoreCaseAndIdNot("Novo", 1L)).thenReturn(false);

        AreaResponse response = service.update(1L, new AreaRequest("Novo", "Descrição", "#112233", "book", 4));

        assertThat(response.name()).isEqualTo("Novo");
        assertThat(response.description()).isEqualTo("Descrição");
        assertThat(response.color()).isEqualTo("#112233");
        assertThat(response.icon()).isEqualTo("book");
        assertThat(response.position()).isEqualTo(4);
    }

    @Test
    void atualizarAreaInexistenteLancaNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("Qualquer", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizarParaNomeDeOutraAreaLancaConflito() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Area()));
        when(repository.existsByNameIgnoreCaseAndIdNot("CLOUD", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("CLOUD", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void arquivaEDesarquivaArea() {
        Area area = new Area();
        when(repository.findById(1L)).thenReturn(Optional.of(area));

        assertThat(service.archive(1L).archived()).isTrue();
        assertThat(service.unarchive(1L).archived()).isFalse();
    }

    @Test
    void buscarAreaInexistenteLancaNaoEncontrado() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }
}