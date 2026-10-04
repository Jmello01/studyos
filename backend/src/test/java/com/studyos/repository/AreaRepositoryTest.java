package com.studyos.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyos.TestcontainersConfiguration;
import com.studyos.entity.Area;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AreaRepositoryTest {

    @Autowired
    private AreaRepository repository;

    private Area newArea(String name, int position) {
        Area area = new Area();
        area.setName(name);
        area.setPosition(position);
        return area;
    }

    @Test
    void migracaoSemeiaAsNoveAreasIniciais() {
        List<Area> areas = repository.findAllByOrderByPositionAscNameAsc();

        assertThat(areas).hasSize(9);
        assertThat(areas.get(0).getName()).isEqualTo("FACULDADE");
        assertThat(areas.get(2).getName()).isEqualTo("INGLÊS");
        assertThat(areas.get(8).getName()).isEqualTo("LEITURA");
    }

    @Test
    void salvaAreaPreenchendoIdETimestamps() {
        Area saved = repository.saveAndFlush(newArea("Estudos Livres", 10));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isArchived()).isFalse();
    }

    @Test
    void existsByNameIgnoreCaseEncontraNomeComOutraCaixa() {
        assertThat(repository.existsByNameIgnoreCase("faculdade")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("Inexistente")).isFalse();
    }

    @Test
    void existsByNameIgnoreCaseAndIdNotIgnoraOProprioRegistro() {
        Area faculdade = repository.findAllByOrderByPositionAscNameAsc().get(0);

        assertThat(repository.existsByNameIgnoreCaseAndIdNot("FACULDADE", faculdade.getId())).isFalse();
        assertThat(repository.existsByNameIgnoreCaseAndIdNot("FACULDADE", -1L)).isTrue();
    }

    @Test
    void bancoRejeitaNomeDuplicadoMesmoComCaixaDiferente() {
        assertThatThrownBy(() -> repository.saveAndFlush(newArea("faculdade", 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listagemSemArquivadasOcultaAsArquivadas() {
        Area cloud = repository.findAllByOrderByPositionAscNameAsc().stream()
                .filter(a -> a.getName().equals("CLOUD"))
                .findFirst()
                .orElseThrow();
        cloud.setArchived(true);
        repository.saveAndFlush(cloud);

        List<String> ativas = repository.findByArchivedFalseOrderByPositionAscNameAsc().stream()
                .map(Area::getName)
                .toList();

        assertThat(ativas).hasSize(8).doesNotContain("CLOUD");
    }

    @Test
    void findMaxPositionDevolveAMaiorPosicao() {
        assertThat(repository.findMaxPosition()).isEqualTo(9);
    }
}