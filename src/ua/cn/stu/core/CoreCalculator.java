package ua.cn.stu.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CoreCalculator {
	private static final String DESCRIPTION_PROPERTY = "description";
	private static final String DESCRIPTOR_NAME_PART = "descriptor";
	private static final String OPERATOR_PROPERTY = "operator";
	private static final String TYPE_PROPERTY = "type";
	private static final String MAIN_CLASS_PROPERTY = "main.class";
	private static final String PROPERTIES_EXTENTION = ".properties";
	private static final String JAR_EXTENTION = ".jar";
	private static final String PLUGIN_DIR = "src/plugins";

	private static Object executeMethod(Class<?> pluginClass, String methodName, 
			Class<?>[] methodParameterTypes, Object[] methodArguments) throws 
	NoSuchMethodException, IllegalAccessException, InvocationTargetException, 
	InstantiationException
	{
		Method method = pluginClass.getMethod(methodName, methodParameterTypes);
		Object result = method.invoke(pluginClass.newInstance(), methodArguments);
		return result;
	}
	private static boolean isPluginClass(Class<?> pluginClass) {
		boolean isPlugin = false;
		Class<?>[] implementedInterfaces = pluginClass.getInterfaces();
		for (Class<?> implementedInterface : implementedInterfaces) {
			if ("ua.cn.stu.plugin.api.Plugin"
					.equalsIgnoreCase(implementedInterface.getName())) {
				isPlugin = true;
				continue;
			}
		}
		return isPlugin;
	}
	public static void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                System.out.println(fileEntry.getName());
            }
        }
    }
	private static String getDescriptorPath(URL jarURL) throws IOException {
		
		String descpriptorPath = null;
		ZipInputStream zip = new ZipInputStream(jarURL.openStream());
		ZipEntry zipEntry = null;
		while ((zipEntry = zip.getNextEntry()) != null) {
			String entryName = zipEntry.getName();
			if (entryName.contains(DESCRIPTOR_NAME_PART) && 
					entryName.endsWith(PROPERTIES_EXTENTION)) {
				descpriptorPath = entryName;
				continue;
			}
		}
		return descpriptorPath;
	}
	
	private static Map<String, PluginInfo> loadPlugins(File[] jars) throws 
	URISyntaxException, NoSuchMethodException, InvocationTargetException {
		
		Map<String, PluginInfo> pluginClasses = new HashMap<>();
		for (File jar : jars) {
			PluginClassLoader pluginClassloader = null;
			try {
				URL jarURL = jar.toURI().toURL();
				pluginClassloader = new PluginClassLoader(jar.getPath());
				String descpriptorPath = getDescriptorPath(jarURL);
				//////////////
				System.out.println(descpriptorPath);

				
				////////////////
				Properties propertis = new Properties();
				InputStream inputStream = pluginClassloader
						.getResourceAsStream(descpriptorPath);
				propertis.load(inputStream);
				
				String className = propertis.getProperty(MAIN_CLASS_PROPERTY);
				String operationType = propertis.getProperty(TYPE_PROPERTY);
				String operator = propertis.getProperty(OPERATOR_PROPERTY);
				String description = propertis.getProperty(DESCRIPTION_PROPERTY);
				
				Class<?> pluginClass = Class
						.forName(className, false, pluginClassloader);
				boolean isPlugin = isPluginClass(pluginClass);
				if (isPlugin && operationType != null && operator != null 
						&& description != null) {
					executeMethod(pluginClass, "invoke", new Class<?>[] {},
							new Object[] {});
					PluginInfo pluginInfo = new PluginInfo();
					pluginInfo.setClassReference(pluginClass);
					if (OperatorType.UNARY.getOperatorType()
							.equalsIgnoreCase(operationType)) {
						pluginInfo.setOperatorType(OperatorType.UNARY);
					} else if (OperatorType.BINARY.getOperatorType()
							.equalsIgnoreCase(operationType)) {
						pluginInfo.setOperatorType(OperatorType.BINARY);
					}
					pluginInfo.setOperatorType(OperatorType.BINARY);
					pluginInfo.setOperator(operator);
					pluginInfo.setDescription(description);
					pluginClasses.put(pluginInfo.getOperator().toString(), pluginInfo);
				}
			} catch (MalformedURLException ex) {

				ex.printStackTrace();
				
			} catch (ClassNotFoundException ex) {

				ex.printStackTrace();
			} catch (IOException ex) {

				ex.printStackTrace();
			} catch (InstantiationException ex) {

				ex.printStackTrace();
			} catch (IllegalAccessException ex) {

				ex.printStackTrace();
			}
		}
		return pluginClasses;
	}
	private static File[] getAllJarsFromPluginDir() {
		File pluginDir = new File(PLUGIN_DIR);
		//
		listFilesForFolder(pluginDir);
		//
		File[] jars = pluginDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File pathname, String name) {
				// TODO Auto-generated method stub
				return name.endsWith(".jar");
			}
		}); 
		return jars;
	}

	public static void main(String[] args) throws IOException, 
	InstantiationException, IllegalAccessException, URISyntaxException, 
	NoSuchMethodException, SecurityException, IllegalArgumentException, 
	InvocationTargetException {

		File[] jars = getAllJarsFromPluginDir();
		Map<String, PluginInfo> pluginClasses = loadPlugins(jars);
		System.out.println("Supported operations:");
		for (PluginInfo pluginInfo : pluginClasses.values()) {
			System.out.println(pluginInfo.getDescription());
		}
		String input = null;
		while (!"exit".equalsIgnoreCase(input)) {
			System.out.println("Please enter expression or type exit >>>");
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(System.in));
			input = reader.readLine();
			if ("exit".equalsIgnoreCase(input)) {
				System.out.println("Closing calculator");
			} else {
				boolean isMatchToOperation = false;
				for (String operation : pluginClasses.keySet()) {
					if (input.contains(operation)) {
						Pattern  pattern = Pattern.compile(
								"(\\d+)(\\\\" + operation + ")(\\d+)");
						Matcher matcher = pattern.matcher(input);
						if (matcher.matches()) {
							isMatchToOperation = true;
							String firstParameter = matcher.group(1);
							String opearator = matcher.group(2);
							String secondParameter = matcher.group(3);
							Class<?>[] methodParameterTypes = new Class<?>[] {
								double.class, double.class };
								Object[] methodArgument = new Object[] {
										Double.valueOf(firstParameter), Double.valueOf(secondParameter) };
								Double result = (Double) executeMethod(
										pluginClasses.get(opearator).getClassReference(), 
										"calculateBinary", methodParameterTypes, methodArgument);
								System.out.println("The result of opereation " + result);
						}
					}
				}
				if (!isMatchToOperation) {
					System.out.println("Operatio is not supported");
				}
			}
		}
	}
}
