package persistence;

import model.Profile;
import java.io.*;
import java.util.*;

/** DESIGN PATTERN: Singleton — one instance manages all file-based persistence */
public class FileDataManager {
    private static FileDataManager instance;
    private static final String DATA_DIR    = "data/";
    private static final String PROFILES_FILE = DATA_DIR + "profiles.dat";

    private Map<String, Profile> profiles = new HashMap<>();

    private FileDataManager() {
        new File(DATA_DIR).mkdirs();
        loadProfiles();
    }

    public static FileDataManager getInstance() {
        if (instance == null) instance = new FileDataManager();
        return instance;
    }

    @SuppressWarnings("unchecked")
    private void loadProfiles() {
        File f = new File(PROFILES_FILE);
        if (!f.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            profiles = (Map<String, Profile>) ois.readObject();
        } catch (Exception e) {
            profiles = new HashMap<>();
        }
    }

    public void saveProfiles() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PROFILES_FILE))) {
            oos.writeObject(profiles);
        } catch (Exception e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    public boolean registerProfile(String username, String password) {
        if (profiles.containsKey(username)) return false;
        profiles.put(username, new Profile(username, password));
        saveProfiles();
        return true;
    }

    public Profile login(String username, String password) {
        Profile p = profiles.get(username);
        return (p != null && p.authenticate(password)) ? p : null;
    }

    public void saveProfile(Profile profile) {
        profiles.put(profile.getUsername(), profile);
        saveProfiles();
    }

    public List<Profile> getAllProfiles() { return new ArrayList<>(profiles.values()); }
}
