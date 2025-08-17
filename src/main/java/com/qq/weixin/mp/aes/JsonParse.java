/**
 * 对企业微信发送给企业后台的消息加解密示例代码.
 * 
 * @copyright Copyright (c) 1998-2020 Tencent Inc.
 */

// ------------------------------------------------------------------------

package com.qq.weixin.mp.aes;

/**
 * 针对 org.json.JSONObject,
 * 要编译打包架包json
 * 官方源码下载地址 : https://github.com/stleary/JSON-java, jar包下载地址 : https://mvnrepository.com/artifact/org.json/json
 */

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * JsonParse class
 *
 * 提供提取消息格式中的密文及生成回复消息格式的接口.
 */
public class JsonParse {

	/**
	 * 提取出 JSON 包中的加密消息
	 * @param jsontext 待提取的json字符串
	 * @return 提取出的加密消息字符串
	 * @throws AesException 
	 */
	public static Object[] extract(String jsontext) throws AesException     {
		Object[] result = new Object[3];
		try {

			Map<String, String> resultMap = new HashMap<String, String>(16);
			resultMap = parseXmlToMap(jsontext, resultMap);
        	String encrypt_msg = resultMap.get("Encrypt");
			String tousername  = resultMap.get("ToUserName");
			String agentid     = resultMap.get("AgentID");

			result[0] = tousername;
			result[1] = encrypt_msg;
			result[2] = agentid;
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new AesException(AesException.ParseJsonError);
		}
	}

	public static Map<String, String> parseXmlToMap(String xml, Map<String, String> map) {
		try {
			SAXReader reader = new SAXReader();
			Document doc = reader.read(new StringReader(xml));
			Element root = doc.getRootElement();
			String path = "";
			if (map.containsKey(root.getName().trim())) {
				path = map.get(root.getName().trim());
				map.remove(root.getName().trim());
			}
			for (Iterator i = root.elementIterator(); i.hasNext();) {
				Element element = (Element) i.next();
				if (element.isTextOnly()) {
					if (path.length() > 0) {
						map.put(path + element.getName().trim(), element.getTextTrim());
					} else {
						map.put(element.getName().trim(), element.getTextTrim());
					}
				} else {
					map.put(element.getName().trim(), path+ element.getName().trim() + ".");
					parseXmlToMap(element.asXML(), map);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * 生成json消息
	 * @param encrypt 加密后的消息密文
	 * @param signature 安全签名
	 * @param timestamp 时间戳
	 * @param nonce 随机字符串
	 * @return 生成的json字符串
	 */
	public static String generate(String encrypt, String signature, String timestamp, String nonce) {

		String format = "{\"encrypt\":\"%1$s\",\"msgsignature\":\"%2$s\",\"timestamp\":\"%3$s\",\"nonce\":\"%4$s\"}";
		return String.format(format, encrypt, signature, timestamp, nonce);

	}
}
