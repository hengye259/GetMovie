package movieGet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 抓包方法：使用HttpClient 和 Jsoup包
	 * 1.先使用HttpClient连接url对应的网页，然后把网页源码获取下来变成一个String对象
	 * 2.使用Jsoup.parse()方法对此String重构回html格式，再用JQuery选择器形式拿到有用的元素
	 * 3.拿到元素后就可以拿到元素的属性，html值，整个标签加html值等等。。。
 * 
 * 本次程序思路：
	 * 1.先要去网页检查元素，找到这100部电影的电影链接---------getLink()方法内
	 * 2.找到这些链接后，找到一条链接就再用httpclient进入该链接，拿到下载链接----------getLink()方法内执行getDownLoadLink();
	 * 3.找到的有用的a标签的下载链接属性,再把电影名和属性值就是下载链接放进一个Map里面
	 * 4.最后从Map中把数据显示在JTable上
 * @author josia
 *
 */
public class MovieSerachUtil {

	private Main main;
	
	public MovieSerachUtil(Main main) {
		this.main = main;
	}
	
	Map<String,String> linkMap = new HashMap<String, String>();
	String url = "http://www.dy2018.com";

	HttpGet get = null;//用于发起get请求
	HttpResponse resp = null;//网页的响应对象
	String content = null;//响应回来的html源码存放于这
	Document doc = null;//用Jsoup创建的Dom对象
	Elements elements = null;//相当于JQuery对象
	HttpClient client = new DefaultHttpClient();

	/**
	 * 定期更新
	 * 从网上更新一次记录一次时间
	 * 如果下次点击的时间也记录的时间小于两天就不更新
	 * 如果大于两天更新，时间会记录成这一次时间
	 * @return
	 */
	public boolean checkTime() {
		File file = new File("time.txt");
		FileWriter fw = null;
		FileReader fr = null;
		BufferedReader br = null;

		if (!file.exists()) {
			try {
				file.createNewFile();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			fr = new FileReader(file);
			br = new BufferedReader(fr);
			String time = null;
			time = br.readLine();
			int now = (int) new Date().getTime();
			int oldTime = 0;
			if (time != null) {
				long s = Long.parseLong(time);
				oldTime = (int) s;
				if (oldTime == 0 || time.length() == 0) {
					fw = new FileWriter(file);
					fw.write(String.valueOf(now));
					return true;
				} else if (now - oldTime > 1000 * 60 * 60 * 24 * 2) {
					fw = new FileWriter(file);
					fw.write(String.valueOf(now));
					return true;
				} else {
					return false;
				}

			}else {
				fw = new FileWriter(file);
				fw.write(String.valueOf(now));
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}

	/**
	 * 本地缓存，原理把map序列化
	 * 速度更快，但不一定是最新的内容
	 * 如果想更新，需要手动把time.txt文件中的时间删除保存
	 * @param map
	 */
	public void saveData(Map<String,String> map) {
		ObjectOutputStream oos = null;

		// 把map存进文件
		File file = new File("link.txt");
		if (!file.exists()) {
			try {
				file.createNewFile();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(map);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 获取本地缓存，原理把文件中的map反序列化
	 * @return
	 */
	public Map<String,String> getData() {
		File file = new File("link.txt");
		ObjectInputStream ois = null;
		Map<String,String> map = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(file));
			map = (HashMap<String, String>) ois.readObject();
			return map;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}

	public Map<String, String> getDownLoadLinkMap() {
		return this.linkMap;
	}

	public void getLink() {

		try {

			get = new HttpGet(url + "/html/gndy/dyzz/index.html");
			resp = client.execute(get);
			content = EntityUtils.toString(resp.getEntity(), "GBK");
			doc = Jsoup.parse(content);
			elements = doc.select("a.ulink");
			System.out.println(elements.size());
			for (Element e : elements) {
				// firstMap.put(e.text(), e.attr("href"));
				getDownLoadLink(e.attr("href"));
			}

			for (int i = 2; i < 5; i++) {
				get = new HttpGet("http://www.dy2018.com/html/gndy/dyzz/index_" + i + ".html");
				resp = client.execute(get);
				content = EntityUtils.toString(resp.getEntity(), "GBK");
				doc = Jsoup.parse(content);
				elements = doc.select("a.ulink");
				System.out.println(elements.size());
				for (Element e : elements) {
					// firstMap.put(e.text(), e.attr("href"));
					getDownLoadLink(e.attr("href"));
				}

				saveData(linkMap);
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void getDownLoadLink(String href) {

		get = new HttpGet(url + href);

		try {

			resp = client.execute(get);
			content = EntityUtils.toString(resp.getEntity(), "GBK");
			doc = Jsoup.parse(content);
			// System.out.println(content);
			elements = doc.select("td[style=WORD-WRAP: break-word] a");

			// System.out.println(elements.size());
			for (Element e : elements) {
				String ftpLink = e.attr("href");
				String[] movie = ftpLink.split("]");
				String movieName = movie[1];
				linkMap.put(movieName, ftpLink);
			}

		} catch (ParseException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}
	

}
