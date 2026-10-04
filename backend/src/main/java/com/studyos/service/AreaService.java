package com.studyos.service;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.entity.Area;
import com.studyos.exception.ConflictException;
import com.studyos.exception.ResourceNotFoundException;
import com.studyos.mapper.AreaMapper;
import com.studyos.repository.AreaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AreaService {

    private final AreaRepository repository;
    private final AreaMapper mapper;

    public AreaService(AreaRepository repository, AreaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> list(boolean includeArchived) {
        List<Area> areas = includeArchived
                ? repository.findAllByOrderByPositionAscNameAsc()
                : repository.findByArchivedFalseOrderByPositionAscNameAsc();
        return areas.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AreaResponse get(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public AreaResponse create(AreaRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Já existe uma área com o nome '" + request.name() + "'");
        }
        Area area = mapper.toEntity(request);
        if (request.position() == null) {
            area.setPosition(repository.findMaxPosition() + 1);
        }
        return mapper.toResponse(repository.save(area));
    }

    @Transactional
    public AreaResponse update(Long id, AreaRequest request) {
        Area area = findOrThrow(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Já existe uma área com o nome '" + request.name() + "'");
        }
        mapper.updateEntity(area, request);
        return mapper.toResponse(area);
    }

    @Transactional
    public AreaResponse archive(Long id) {
        return setArchived(id, true);
    }

    @Transactional
    public AreaResponse unarchive(Long id) {
        return setArchived(id, false);
    }

    private AreaResponse setArchived(Long id, boolean archived) {
        Area area = findOrThrow(id);
        area.setArchived(archived);
        return mapper.toResponse(area);
    }

    private Area findOrThrow(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área " + id + " não encontrada"));
    }
}