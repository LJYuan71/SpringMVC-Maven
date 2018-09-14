package tk.ljyuan71.common.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.velocity.VelocityContext;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;


/**
 * Mybatis代码自动生成
 * @author ljyuan 2018年9月12日
 * @Description:  
 */
public class MybatisGeneratorUtil {
	
	// generatorConfig模板路径
	private static String generatorConfig_vm = "/template/generatorConfig.vm";
	// Service模板路径
	private static String service_vm = "/template/Service.vm";
	// ServiceImpl模板路径
	private static String serviceImpl_vm = "/template/ServiceImpl.vm";
	// Controller模板路径
	private static String controller_vm = "/template/Controller.vm";

	/**
	 * 代码生成：先生成generatorConfig.xml文件，然后使用官方模板生成对应的文件。
	 * 其他业务控制层和业务成生成是通过自己定义的模板生成
	 * @param tableNames	一个模块下的数据库表
	 * @param packageName	包名 例：tk.ljyuan71
	 * @param module		项目模块 common 完整路径packageName+module
	 *  
	 */
	public static void generator(Set<String> tableNames,String packageName,String module) throws Exception{

		String targetProject = module + "/" + module + "-dao";
		String basePath = MybatisGeneratorUtil.class.getResource("/").getPath().replace("/target/classes/", "").replace(targetProject, "");
		if (SystemUtils.IS_OS_WINDOWS) {//win系统
			generatorConfig_vm = MybatisGeneratorUtil.class.getResource(generatorConfig_vm).getPath().replaceFirst("/", "");
			service_vm = MybatisGeneratorUtil.class.getResource(service_vm).getPath().replaceFirst("/", "");
			serviceImpl_vm = MybatisGeneratorUtil.class.getResource(serviceImpl_vm).getPath().replaceFirst("/", "");
			basePath = basePath.replaceFirst("/", "");
		} else {
			generatorConfig_vm = MybatisGeneratorUtil.class.getResource(generatorConfig_vm).getPath();
			service_vm = MybatisGeneratorUtil.class.getResource(service_vm).getPath();
			serviceImpl_vm = MybatisGeneratorUtil.class.getResource(serviceImpl_vm).getPath();
		}

		String generatorConfigXml = MybatisGeneratorUtil.class.getResource("/").getPath().replace("/target/classes/", "") + "/src/main/resources/generatorConfig.xml";
		targetProject = basePath + targetProject;

		System.out.println("========== 开始生成generatorConfig.xml文件 ==========");
		List<Map<String, Object>> tables = new ArrayList<>();
		try {
			VelocityContext context = new VelocityContext();
			Map<String, Object> table;
			//读取对应配置文件的配置
			String jdbcDriver = SysConfigUtil.getInstance("generator").get("generator.jdbc.driver"); 
			String jdbcUrl = SysConfigUtil.getInstance("generator").get("generator.jdbc.url");
			String jdbcUsername = SysConfigUtil.getInstance("generator").get("generator.jdbc.username"); 
			String jdbcPassword = SysConfigUtil.getInstance("generator").get("generator.jdbc.password");
			//数据库方言
			String sql;
			String dialect;
			if(StringUtils.containsIgnoreCase(jdbcDriver, "mysql")){
				dialect = "mysql";
				sql = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_name in ('"+StringUtils.join(tableNames, "','")+"')";
			} else if(StringUtils.containsIgnoreCase(jdbcDriver, "oracle")){
				dialect = "oracle";
				sql = "select table_name from user_tables where status='VALID' and table_name in ('"+StringUtils.join(tableNames, "','")+"')";
			} else {
				throw new Exception("不支持的数据库驱动");
			}
			// 查询定制前缀项目的所有表
			JdbcUtil jdbcUtil = new JdbcUtil(jdbcDriver, jdbcUrl, jdbcUsername, jdbcPassword);
			List<Map<String, Object>> result = jdbcUtil.selectByParams(sql, null);
			for (Map<String, Object> map : result) {
				System.out.println("数据库表："+map.get("TABLE_NAME"));
				table = new HashMap<>();
				table.put("table_name", map.get("TABLE_NAME"));
				table.put("model_name", lineToHump(ObjectUtils.toString(map.get("TABLE_NAME"))));
				tables.add(table);
			}
			jdbcUtil.release();

			context.put("tables", tables);
			context.put("dialect", dialect);
			context.put("generator_javaModelGenerator_targetPackage", packageName + "." + module + ".model");
			context.put("generator_sqlMapGenerator_targetPackage", module + ".mapper");
			context.put("generator_javaClientGenerator_targetPackage", packageName + "." + module + ".dao.mapper");
			VelocityUtil.generate(generatorConfig_vm, generatorConfigXml, context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("========== 结束生成generatorConfig.xml文件 ==========");

		System.out.println("========== 开始运行MybatisGenerator ==========");
		List<String> warnings = new ArrayList<>();
		File configFile = new File(generatorConfigXml);
		ConfigurationParser cp = new ConfigurationParser(warnings);
		Configuration config = cp.parseConfiguration(configFile);
		DefaultShellCallback callback = new DefaultShellCallback(true);
		MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config, callback, warnings);
		myBatisGenerator.generate(null);
		for (String warning : warnings) {
			System.out.println(warning);
		}
		System.out.println("========== 结束运行MybatisGenerator ==========");

		System.out.println("========== 开始生成Service ==========");
		String ctime = new SimpleDateFormat("yyyy/M/d").format(new Date());//创建时间
		String servicePath = basePath + "/src/main/java/" + packageName.replaceAll("\\.", "/") + "/" + module + "/service";
		String serviceImplPath = basePath + "/src/main/java/" + packageName.replaceAll("\\.", "/") + "/" + module + "/service/impl";
		String controllerPath = basePath + "/src/main/java/" + packageName.replaceAll("\\.", "/") + "/" + module + "/controller";
		System.out.println("servicePath:"+servicePath);
		System.out.println("serviceImplPath:"+serviceImplPath);
		System.out.println("controllerPath:"+controllerPath);
		for (int i = 0; i < tables.size(); i++) {
			String model = lineToHump(ObjectUtils.toString(tables.get(i).get("table_name")));
			String service = servicePath + "/" + model + "Service.java";
			String serviceImpl = serviceImplPath + "/" + model + "ServiceImpl.java";
			String controller = controllerPath + "/" + model + "Controller.java";
			VelocityContext context = new VelocityContext();
			context.put("package_name", packageName);
			context.put("model", model);
			context.put("module", module);
			context.put("mapper",toLowerCaseFirstOne(model));
			context.put("ctime", ctime);
			// 生成service
			VelocityUtil.generate(service_vm, service, context);
			// 生成serviceImpl
			VelocityUtil.generate(serviceImpl_vm, serviceImpl, context);
			// 生成serviceImpl
			VelocityUtil.generate(controller_vm, controller, context);
		}
		System.out.println("========== 结束生成Service ==========");
	}

	// 递归删除非空文件夹
	public static void deleteDir(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				deleteDir(files[i]);
			}
		}
		dir.delete();
	}
	
	/**
     * 首字母转小写
     * @param s
     * @return
     */
    public static String toLowerCaseFirstOne(String s) {
        if (StringUtils.isBlank(s)) {
            return s;
        }
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }
    
    /***
	 * 下划线命名转为驼峰命名首字母大写
	 * 例：ZB_GONGCHENG --> ZbGongcheng
	 */
	public static String lineToHump(String para){
	    StringBuilder result=new StringBuilder();
	    String a[]=para.split("_");
	    for(String s:a){
	        result.append(s.substring(0, 1).toUpperCase());
	        result.append(s.substring(1).toLowerCase());
	    }
	    return result.toString();
	}
	
	public static void main(String[] args) throws Exception {
		//一个模块下的表
		Set<String> tableNames = new HashSet<String>();
		tableNames.add("zb_gc");
		generator(tableNames, "tk.ljyuan71", "zbgc");
	}
	
	

}
