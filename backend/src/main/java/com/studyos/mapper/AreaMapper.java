package com.studyos.mapper;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.entity.Area;
import org.springframework.stereotype.Component;

@Component
public class AreaMapper {

    public AreaResponse toResponse(Area area) {
        return new AreaResponse(
                area.getId(),
                area.getName(),
                area.getDescription(),
                area.getColor(),
                area.getIcon(),
                area.getPosition(),
                area.isArchived());
    }

    public Area toEntity(AreaRequest request) {
        Area area = new Area();
        updateEntity(area, request);
        return area;
    }

    public void updateEntity(Area area, AreaRequest request) {
        area.setName(request.name());
        area.setDescription(request.description());
        area.setColor(request.color());
        area.setIcon(request.icon());
        if (request.position() != null) {
            area.setPosition(request.position());
        }
    }
}