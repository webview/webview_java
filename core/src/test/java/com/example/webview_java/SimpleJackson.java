package com.example.webview_java;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** jackson 简单工具
 * @author wanne
 */
public class SimpleJackson {

	private static ObjectMapper MAPPER = new ObjectMapper();
	
	static {
		// new datetime
        JavaTimeModule module=new JavaTimeModule();
        module.addSerializer(LocalDateTime.class,new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        module.addDeserializer(LocalDateTime.class,new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        module.addSerializer(LocalDate.class,new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        module.addDeserializer(LocalDate.class,new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        MAPPER.registerModule(module);
		// custom serializer formatter (In order to resolve array is not in new line)
		MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		// 格式化输出的设置
		DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		printer.indentObjectsWith(indenter);
		printer.indentArraysWith(indenter);
		MAPPER.setDefaultPrettyPrinter(printer);
	}
	
	/** 转字符串
	 * @param obj
	 * @return
	 * @throws JsonProcessingException
	 */
	public static String toJsonString(Object obj) 
			throws JsonProcessingException {
		if (MAPPER.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
			MAPPER.disable(SerializationFeature.INDENT_OUTPUT); // 关闭格式化输出
		}
		return MAPPER.writeValueAsString(obj);
	}

	/** 转字符串
	 * @param obj
	 * @return
	 * @throws JsonProcessingException
	 */
	public static String toPrettyJsonString(Object obj)
			throws JsonProcessingException {
		if (!MAPPER.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
			MAPPER.enable(SerializationFeature.INDENT_OUTPUT); // 启用格式化输出
		}
		return MAPPER.writeValueAsString(obj);
	}
	
	/** 转对象
	 * @param <T>
	 * @param json
	 * @param clazz
	 * @return
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	public static <T> T toObject(String json, Class<T> clazz) 
			throws JsonMappingException, JsonProcessingException {
		return MAPPER.readValue(json, clazz);
	}
	
	public static JsonNode toJson(String json)
			throws JsonProcessingException {
		return MAPPER.readTree(json);
	}

	public static void setMapper(ObjectMapper mapper) {
		MAPPER = mapper;
	}

	
	// ===== jackson 链式表达式调用， 2022-05-16 =====
	// 分隔符
	private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
	//  检查数组， 例如：[4]
	private static final Pattern ARRAY_PATTERN = Pattern.compile(".+\\[[0-9]+\\]$");
	
	/** 使用简单的链式表达式获取子节点，某个节点不满足时，直接返回 null。
	 * @param root
	 * @param expr
	 * @return
	 */
	public static JsonNode getByExpression(JsonNode root, String expr) {
		return getByExpression(root, expr, true);
	}
	
	/** 使用简单的链式表达式获取子节点。
	 * 类似 js 中的调用， 但是 '[]' 只表示数组
	 * @param root
	 * @param expr
	 * @param safe true - 会抛出 NullPointerException, false - 在应该产生 NullPointerException 时，返回 null
	 * @throws NullPointerException 中间节点为 null 时会抛出异常
	 * @return
	 */
	public static JsonNode getByExpression(JsonNode root, String expr, boolean safe) 
		throws NullPointerException {
		if(root.isEmpty()) {
			return null;
		}
		
//		String[] arr = { "abc[1]", "abc[1a]", "abc[[1]", "[122]", "a[1]dd", "a[1]c" };
//		for (String s : arr) {
//			System.out.println(s + ": " + ARRAY_PATTERN.matcher(s).matches());
//		}
		
		JsonNode parent = root;
		List<String> nodeNames = splitDotExpression(expr, true);
		
		List<String> passedNames = new ArrayList<>(); 
		for (String name : nodeNames) {
			JsonNode node = null;
			
			if(ARRAY_PATTERN.matcher(name).matches()) { // 数组
				int indexStart = name.lastIndexOf('[') + 1;
				String nameArr = name.substring(0, indexStart - 1);
				int indexArr = Integer.parseInt(name.substring(indexStart, name.length()-1));
				
				// 数组节点
				JsonNode nodeArr = parent.get(nameArr);
				passedNames.add(nameArr);
				
				// 节点不存在时用 null 判断，isEmpty() 不是判断节点是否存在，而是节点是否没有值
				// 比如：字符串数字中的元素，是没有值的 isEmpty() 会返回 true
				if(nodeArr == null) {
					if(safe) {
						return null;
					}
					throw new NullPointerException(String.join(".", passedNames));
				}
				
				// 数组元素节点
				node = nodeArr.get(indexArr);
				passedNames.set(passedNames.size()-1, name);
			} else { // 普通属性
				node = parent.get(name);
				passedNames.add(name+".");
			}
			
			parent = node;
			if(node == null) { 
				if(safe) {
					return null;
				}
				throw new NullPointerException(String.join(".", passedNames));
			}
		}
		
		return parent;
	}
	
	/** 根据符号 '.' 分割字符串, '\.' 一起出现表示对 '.' 的转义-不会被分割， '\' 本身不需要转义。<br>
	 * "a.b.d.ef"   => "a", "b", "d", "ef" <br>
	 * "a.b.d\.ef"   => "a", "b", "d.ef" <br>
	 * "a.b.\\\\ef"   => "a", "b", "\\\\ef" <br>
	 * @param expression
	 * @param ignoreBlank 是否舍弃空的或者全空白字符串
	 * @return
	 */
	private static List<String> splitDotExpression(String expression, boolean ignoreBlank) {
		String[] arr = DOT_PATTERN.split(expression);
		List<String> list = new ArrayList<>(Arrays.asList(arr));
		int index = 0;
		while(index != list.size()-1) {
			String s = list.get(index);
			if( s.endsWith("\\") ) {
				// 去掉 index 转义符，补充 . ，拼接 index+1
				s = s.substring(0, s.length()-1) + "." + list.get(index+1);
				
				list.set(index, s);
				list.remove(index+1);
			} else {
				index++;
			}
		}
		
		
		return !ignoreBlank ? list : list.stream()
				.filter(e -> !Objects.equals("", e.trim()))
				.collect(Collectors.toList());
	}
	
	public static void main(String[] args) throws JsonProcessingException {
		String str = "{\"pageNo\":2,\"currentPage\":1,\"pageSize\":11,\"totalCount\":60,\"showTotalCount\":0,\"pageCount\":6,\"results\":[{\"id\":\"40289fb4809c3b480180a01ba732002e\",\"createTime\":1651953608528,\"modifyTime\":1651953608528,\"originId\":\"117189\",\"title\":\"“农业面源、重金属污染防控和绿色投入品研发”重点专项2021年度部省联动项目答辩评审专家名单公告\",\"data\":[1,2,3,4]},{\"id\":\"40289fb48045ddef018076e8c1ea06b9\",\"createTime\":1651262407156,\"modifyTime\":1651262407156,\"originId\":\"116540\",\"title\":\"科技部关于发布国家重点研发计划“干细胞研究与器官修复”等重点专项2022年度项目申报指南的通知\"}]}";
		
		JsonNode json = toJson(str);
		System.out.println(json.getNodeType()+","+json.getClass().getName());
		
		JsonNode arr = getByExpression(json, "results");
		System.out.println(arr.getNodeType()+","+arr.getClass().getName());
		System.out.println(arr);
		
		JsonNode arr2 = getByExpression(json, "results[0].data[2]");
		System.out.println(arr2.getNodeType()+","+arr2.getClass().getName());
		System.out.println(arr2);
	}

}
