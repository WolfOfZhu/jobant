package nantian.isrp.task;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;

/**
 **************************************************************** 
 * 公司名称 :广州南天电脑系统有限公司 系统名称 :jobant 功能描述 :TODO 业务描述 : 作 者 名 :@sky_zhu 开发日期
 * :2018年4月9日 下午2:52:26
 ***************************************************************
 * 修改日期 : 修 改 者 : 修改内容 :
 ***************************************************************
 */
public class Test {
	public static void main(String[] args) {
		
		String time_m = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date());
		System.out.println(time_m);
		String curMonth = new SimpleDateFormat("YYYYMM").format(new Date());
		System.out.println(curMonth);
		File baseDir = new File("E:\\test"); // 根目录
		if (!baseDir.exists() || baseDir.isFile()) {
			return;
		}
		// 查找此目录是否存在三级目录（日期目录）

		FileFilter oneDir = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}
		};
		File[] dirs = baseDir.listFiles(oneDir); // 二级目录(渠道)
		if (dirs != null) {
			FileFilter towDir = new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory();
				}
			};
			for (File tmp : dirs) {
				File[] three = tmp.listFiles(towDir); // 三级目录(渠道)
				if (three != null) {
					for (File ff : three) {
						System.out.println(ff.getName().substring(0,6));
							if(ff.getName()!=null&&curMonth.equals((ff.getName().trim()).substring(0,6))){
								System.out.println("--------------");
								Collection<File> listFile = FileUtils.listFiles(
										new File(ff.getAbsolutePath()),
										FileFilterUtils.suffixFileFilter("pdf"),
										DirectoryFileFilter.INSTANCE);
								for(File aa:listFile){
									System.out.println(aa.getAbsolutePath()+"aaaaaaaaaaaaaaaa");
								}
							}
					}
				}
			}
		} else {

		}

		return;
	}
}
