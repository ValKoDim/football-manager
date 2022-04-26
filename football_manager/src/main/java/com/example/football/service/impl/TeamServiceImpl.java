package com.example.football.service.impl;

import com.example.football.models.dto.ImportTeamDTO;
import com.example.football.models.entity.Team;
import com.example.football.models.entity.Town;
import com.example.football.repository.TeamRepository;
import com.example.football.repository.TownRepository;
import com.example.football.service.TeamService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;



@Service
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;


    private final Gson gson;
    private final Validator validator;
    private final ModelMapper modelMapper;
    private final TownRepository townRepository;


    @Autowired
    public TeamServiceImpl(TeamRepository teamRepository, TownRepository townRepository) {
        this.teamRepository = teamRepository;
        this.townRepository = townRepository;

        this.gson = new GsonBuilder().create();

        this.validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();

        this.modelMapper = new ModelMapper();

    }


    @Override
    public boolean areImported() {
        return this.teamRepository.count() > 0;

    }

    @Override
    public String readTeamsFileContent() throws IOException {
        Path path = Path.of("src","main","resources","files","json","teams.json");
        return Files.readString(path);


    }


    @Override
    public String importTeams() throws IOException {

        String json = this.readTeamsFileContent();

        ImportTeamDTO[] teamsDTO = this.gson.fromJson(json, ImportTeamDTO[].class);

        List<String> result = new ArrayList<>();

        for (ImportTeamDTO importTeamDTO:teamsDTO) {

            Set<ConstraintViolation<ImportTeamDTO>> errors = this.validator.validate(importTeamDTO);


            Optional<Team> optTeam = this.teamRepository.findByName(importTeamDTO.getName());


            if (optTeam.isEmpty() &&errors.isEmpty()){
                Team team = this.modelMapper.map(importTeamDTO, Team.class);
                Optional<Town> town = this.townRepository.findByName(importTeamDTO.getTownName());


                town.ifPresent(team::setTown);

                this.teamRepository.save(team);

                result.add("Successfully imported Team " + team);
            }
            else {
                result.add("Invalid Team");
            }
        }
        return String.join("\n",result);
    }
}
