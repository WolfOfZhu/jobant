package nantian.order.util;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 **************************************************************** 
 * 公司名称 :广州南天电脑系统有限公司 
 * 系统名称 :checkxml 
 * 功能描述 :工具类
 * 业务描述 :工单的工具类 
 * 作 者 名 :@sky_zhu
 * 开发日期 :2018年1月10日 下午3:41:50
 ***************************************************************
 * 修改日期 :
 *  修 改 者 : 
 *  修改内容 :
 ***************************************************************
 */
public class OrderUtil {
	/**
	 * 
	 * @author zhuchaopei
	 * @DateTime 2018年1月10日 下午3:43:02
	 * @serverCode 服务代码
	 * @serverComment 服务注解:BigDecimal转成String
	 *
	 * @param obj
	 * @return
	 */
	public OrderUtil() {
	}

	public static String bigToString(Object obj) {
		String flag = "";
		if (obj instanceof String) {
			String string = (String) obj;
			if (string != null && !"".equals(string.trim())
					&& !"null".equalsIgnoreCase(string)) {
				flag = string;
			}
		}
		if (obj instanceof BigDecimal) {
			BigDecimal temp = (BigDecimal) obj;
			if (temp != null) {
				flag = temp.stripTrailingZeros().toPlainString();
			}
		}
		return flag;
	}

	/**
	 * 
	 * @author zhuchaopei
	 * @DateTime 2018年1月10日 下午3:44:07
	 * @serverCode 服务代码
	 * @serverComment 服务注解:当天具体的时间的秒数
	 *
	 * @param tmpTime
	 * @return
	 */
	public static Long timeSecond(String tmpTime) {
		SimpleDateFormat format = new SimpleDateFormat("HHmmss");
		try {
			Date date2 = format.parse(tmpTime);
			return date2.getTime() / 1000;
		} catch (ParseException e) {
			return 0L;
		}
	}

	/**
	 * 
	 * @author zhuchaopei
	 * @DateTime 2018年1月10日 下午3:45:28
	 * @serverCode 服务代码
	 * @serverComment 服务注解:yyyyMMddHHmmss
	 * 与当前的时间的差是否大于等于mins分钟
	 *
	 * @param tmpTime
	 * @param mins
	 * @return
	 */
	public static boolean timeDiff(String tmpTime, String mins) {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		try {
			int seconds = Integer.parseInt(mins) * 60;
			Date tmp = format.parse(tmpTime);
			int toMin = (int) ((System.currentTimeMillis() / 1000) - (tmp
					.getTime() / 1000));
			if (toMin >= seconds) {
				return true;
			}
		} 
		catch (NumberFormatException e) {
			return false;
			} 
		catch (ParseException e) {
			return false;
			}
		return false;
	}

	/**
	 * 
	 * @author zhuchaopei
	 * @DateTime 2018年1月10日 下午3:48:28
	 * @serverCode 服务代码
	 * @serverComment 服务注解:简单对object类型判空操作
	 *
	 * @param obj
	 * @return TRUE为非空，false为空
	 */
	public static boolean isNotEmpty(Object obj) {
		boolean flag = false;
		if (obj instanceof String) {
			String string = (String) obj;
			if (string != null && !"".equals(string.trim())
					&& !"null".equalsIgnoreCase(string)) {
				flag = true;
			}
		} else if (obj instanceof List) {
			List list = (List) obj;
			if (list != null && list.size() > 0) {
				flag = true;
			}
		} else if (obj instanceof Map) {
			Map map = (Map) obj;
			if (map != null && map.size() > 0) {
				flag = true;
			}
		} else {
			if (obj != null) {
				flag = true;
			}
		}
		return flag;
	}
}
