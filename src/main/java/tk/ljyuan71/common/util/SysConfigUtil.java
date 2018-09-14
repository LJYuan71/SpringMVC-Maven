package tk.ljyuan71.common.util;

import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 资源文件读取工具
 */
public class SysConfigUtil {

    // 当打开多个资源文件时，缓存资源文件
    private static HashMap<String, SysConfigUtil> configMap = new HashMap<String, SysConfigUtil>();
    // 资源文件
    private ResourceBundle resourceBundle = null;
    // 默认资源文件名称
    private static final String NAME = "config";

    // 私有构造方法，创建单例
    private SysConfigUtil(String name) {
        this.resourceBundle = ResourceBundle.getBundle(name);
    }
    /**
     * 第一次时加载默认配置文件config
     */
    public static synchronized SysConfigUtil getInstance() {
        return getInstance(NAME);
    }
    /**
     * 第一次时加载对应配置文件的
     */
    public static synchronized SysConfigUtil getInstance(String name) {
        SysConfigUtil conf = configMap.get(name);
        if (null == conf) {
            conf = new SysConfigUtil(name);
            configMap.put(name, conf);
        }
        return conf;
    }

    // 根据key读取value
    public String get(String key) {
        try {
            String value = resourceBundle.getString(key);
            return value;
        } catch (MissingResourceException e) {
            return "";
        }
    }

    // 根据key读取value(整型)
    public Integer getInt(String key) {
        try {
            String value = resourceBundle.getString(key);
            return Integer.parseInt(value);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    // 根据key读取value(布尔)
    public boolean getBool(String key) {
        try {
            String value = resourceBundle.getString(key);
            if ("true".equals(value)) {
                return true;
            }
            return false;
        } catch (MissingResourceException e) {
            return false;
        }
    }
    
    public static void main(String[] args) {
    	String jdbcDriver = SysConfigUtil.getInstance("generator").get("generator.jdbc.driver");
    	System.out.println(jdbcDriver);
	}

}
