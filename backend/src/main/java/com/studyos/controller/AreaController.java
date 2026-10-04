package com.studyos.controller;

import com.studyos.dto.AreaRequest;
import com.studyos.dto.AreaResponse;
import com.studyos.service.AreaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService service;

    public AreaController(AreaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AreaResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(includeArchived);
    }

    @GetMapping("/{id}")
    public AreaResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<AreaResponse> create(@Valid @RequestBody AreaRequest request) {
        AreaResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/areas/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AreaResponse update(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    public AreaResponse archive(@PathVariable Long id) {
        return service.archive(id);
    }

    @PatchMapping("/{id}/unarchive")
    public AreaResponse unarchive(@PathVariable Long id) {
        return service.unarchive(id);
    }
}