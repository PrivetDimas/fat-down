package com.brb.fatdown.repo;

import com.brb.fatdown.model.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ProfileRepository {
    private final Path baseDir;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT);

    public ProfileRepository() {
        String userHome = System.getProperty("user.home");
        baseDir = Path.of(userHome, ".fatdown");
        try {
            Files.createDirectories(baseDir.resolve("profiles"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Profile> loadAllProfiles() {
        try {
            File dir = baseDir.resolve("profiles").toFile();
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files == null) return new ArrayList<>();
            List<Profile> out = new ArrayList<>();
            for (File f : files) {
                try {
                    Profile p = mapper.readValue(f, Profile.class);
                    out.add(p);
                } catch (IOException _) {}
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void saveProfile(Profile p) {
        try {
            Path file = baseDir.resolve("profiles").resolve(p.getId() + ".json");
            mapper.writeValue(file.toFile(), p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Profile> loadLastUsedProfile(List<Profile> all) {
        try {
            Path f = baseDir.resolve("last-used.txt");
            if (!Files.exists(f)) return Optional.empty();
            String id = Files.readString(f).trim();
            return all.stream().filter(p -> p.getId().equals(id)).findFirst();
        } catch (IOException _) {
            return Optional.empty();
        }
    }

    public void setLastUsedProfileId(String id) {
        try {
            Files.writeString(baseDir.resolve("last-used.txt"), id, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
