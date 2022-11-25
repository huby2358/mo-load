package io.mo.util;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;

public class YamlUtil {
    private Map info;

    public Map getInfo(String filename) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        URL url = YamlUtil.class.getClassLoader().getResource(filename);
        if (url != null) {
            this.info = (Map) yaml.load(new FileInputStream(url.getFile()));
            //System.out.println(url.getFile());
        }
        return info;
    }
}
