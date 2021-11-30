package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.domain.Stakeholder;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("stakeholders")
public class StakeholderController {

    private static final Logger logger = LogManager.getLogger(StakeholderController.class);

    private final CrudItemService<Stakeholder> stakeholderService;

    @Autowired
    public StakeholderController(CrudItemService<Stakeholder> stakeholderService) {
        this.stakeholderService = stakeholderService;
    }

    @GetMapping("{id}")
    public ResponseEntity<Stakeholder> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(stakeholderService.get(id), HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<Stakeholder> create(@RequestBody Stakeholder stakeholder) {
        return new ResponseEntity<>(stakeholderService.add(stakeholder), HttpStatus.CREATED);
    }

    @PutMapping("{id}")
    public ResponseEntity<Stakeholder> update(@PathVariable("id") String id, @RequestBody Stakeholder stakeholder) throws ResourceNotFoundException {
        return new ResponseEntity<>(stakeholderService.update(id, stakeholder), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Stakeholder> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(stakeholderService.delete(id), HttpStatus.OK);
    }
}
