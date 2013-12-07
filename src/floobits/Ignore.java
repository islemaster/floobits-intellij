package floobits;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;


class Ignore {
    static String[] IGNORE_FILES = {".gitignore", ".hgignore", ".flignore", ".flooignore"};
    // TODO: make this configurable
    static String[] HIDDEN_WHITELIST = {".gitignore", ".hgignore", ".flignore", ".flooignore", ".floo"};

    //TODO: grab global git ignores:
    //gitconfig_file = popen("git config -z --get core.excludesfile", "r");
    static String[] DEFAULT_IGNORES = {"extern", "node_modules", "tmp", "vendor"};
    static int MAX_FILE_SIZE = 1024 * 1024 * 5;

    protected File path;
    protected Ignore parent;
    protected ArrayList<Ignore> children = new ArrayList<Ignore>();

    protected ArrayList<File> files = new ArrayList<File>();
    protected Integer size = 0;

    protected HashMap<String, List<String>> ignores = new HashMap<String, List<String>>();

    public Ignore (File basePath, Ignore parent, Boolean recurse) {
        this.path = basePath;
        this.parent = parent;
        this.ignores.put("/TOO_BIG/", new ArrayList<String>());

        File[] files = this.path.listFiles();
        for (File file : files) {
            try {
                if (FileUtils.isSymlink(file)) {
                    continue;
                }
                if (recurse && file.isDirectory()) {
                    this.children.add(new Ignore(file, this, recurse));
                    continue;
                }
            } catch (Exception e) {
                continue;
            }
        }

        Flog.debug("Initializing ignores for %s", this.path);
        for (String name : IGNORE_FILES) {
            name = FilenameUtils.concat(this.path.getPath(), name);
            File ignoreFile = new File(name);
            this.loadIgnore(ignoreFile);
        }
    }

    protected void loadIgnore (File file) {
        String ignores[];
        try {
            ignores = FileUtils.readFileToString(file, "UTF-8").split("\\r?\\n");
        } catch (IOException e) {
            return;
        }

        ArrayList igs = new ArrayList<String>();
        for (String ignore: ignores) {
            ignore = ignore.trim();
            if (ignore.length() == 0 || ignore.startsWith("#")) {
                continue;
            }
            igs.add(ignore);
        }

        this.ignores.put(file.getName(), igs);
    }

    protected void addFile (File file) {
        
    }

    public void buildIgnores () {
        
    }

    public Boolean isIgnored (String path) {
        return false;
    }
}
