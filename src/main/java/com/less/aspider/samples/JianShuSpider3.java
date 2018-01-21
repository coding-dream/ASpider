package com.less.aspider.samples;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.less.aspider.ASpider;
import com.less.aspider.bean.Page;
import com.less.aspider.bean.Proxy;
import com.less.aspider.bean.Request;
import com.less.aspider.db.DBHelper;
import com.less.aspider.downloader.Downloader;
import com.less.aspider.downloader.HttpConnDownloader;
import com.less.aspider.processor.PageProcessor;
import com.less.aspider.proxy.ProxyProvider;
import com.less.aspider.proxy.SimpleProxyProvider;
import com.less.aspider.samples.bean.JianSpecial;
import com.less.aspider.samples.bean.JianSubscriber;
import com.less.aspider.samples.db.JianSpecialDao;
import com.less.aspider.samples.db.JianSubscriberDao;
import com.less.aspider.scheduler.BDBScheduler;
import com.less.aspider.util.XunProxyManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.less.aspider.util.XunProxyManager.createProxyAuthorization;

/**
 * @author Administrator
 */
public class JianShuSpider3 {

    private static JianSpecialDao jianSpecialDao ;

    private static JianSubscriberDao jianSubscriberDao;

    private static String collectionBaseUrl = "https://api.jianshu.io/v2/collections/%d";

    private static String userBaseUrl = "https://api.jianshu.io/v2/collections/%d/subscribers?page=%d&count=%d";

    private static Object queryCount = 50;

    private static int specialTotalSize = 37950;

    public static void main(String[] args) {
        configDB();
        String authHeader = createProxyAuthorization("ZF20181206870tBMzFp","5268960af3fb4b1cb572dda081e829f1");

        Downloader downloader = new HttpConnDownloader();
        // headers 设置(具有时效性)
        Map<String, String> headers = new HashMap<>();
        headers.put(XunProxyManager.HEADER_PROXY_AUTH, authHeader);
        headers.put("Host", "s0.jianshuapi.com");
        headers.put("X-App-Name", "haruki");
        headers.put("X-App-Version", "3.2.0");
        headers.put("X-Device-Guid", "127051030369235");
        headers.put("X-Timestamp", "1516457698");
        headers.put("X-Auth-1", "80c3678969f6ef48678e45b8ec8cb211");
        downloader.setHeaders(headers);

        ProxyProvider proxyProvider = SimpleProxyProvider.from(new Proxy(XunProxyManager.IP, XunProxyManager.PORT));
        downloader.setProxyProvider(proxyProvider);

        // SimpleEventBus.getInstance().registerDataSetObserver(proxyProvider);
        // SimpleEventBus.getInstance().startWork("F:\\temp.txt", 60, true);

        ASpider.create()
                .pageProcessor(new PageProcessor() {
                    @Override
                    public void process(Page page) {
                        String url = page.getUrl();
                        if (!url.contains("subscribers")) {
                            System.out.println("======> special " + url + " " + page.getRawText());
                            // 专题
                            Request lastRequest = page.getOriginRequest();
                            Integer collectionIndex = (Integer) lastRequest.getExtra("collectionIndex");

                            if (collectionIndex == null) {
                                collectionIndex = 77;
                            }

                            // warn: if errorReturn = false 无须判断
                            if (page.isDownloadSuccess()) {
                                saveSpecial(collectionIndex,page.getRawText());
                            }

                            int nextCollectionIndex = collectionIndex + 1;
                            if (nextCollectionIndex > specialTotalSize) {
                                return;
                            }
                            // 不管page是成功还是失败,都添加下一页的请求到队列中.(因为个别index专题不存在,需要保持递增不中断.)
                            Request request1 = new Request();
                            String nextUrl = String.format(collectionBaseUrl, nextCollectionIndex);
                            request1.setUrl(nextUrl);
                            request1.putExtra("collectionIndex", collectionIndex + 1);
                            request1.setPriority(1);
                            page.addTargetRequest(request1);

                            // 当前专题page下载成功才添加 <专题下的第一页记录,后续的自动判断>
                            if (page.isDownloadSuccess()) {
                                Request request2 = new Request();
                                request2.setUrl(String.format(userBaseUrl, collectionIndex, 1, queryCount));
                                request2.putExtra("subscribersPage",1);
                                request2.putExtra("collectionIndex",collectionIndex);
                                request2.setPriority(-1);
                                page.addTargetRequest(request2);
                            }
                        } else {
                            System.out.println("======> user " + url + " " + page.getRawText());
                            // 专题下的用户(由于该逻辑稍有复杂,默认情况下该次序是交叉进行的(情况一),如:
                            // 情况一: 专题1-> page1 ,专题2-> page1,专题3-> page1, 专题1-> page2, 专题3-> page2,专题2-> page2)
                            // 情况二: (专题1-> page1, page2, page3, 专题2-> page1,page2,page3)
                            // 当然如果设置优先级队列可以保证执行结果为-> 情况二.
                            Request lastRequest = page.getOriginRequest();
                            Integer subscribersPage = (Integer) lastRequest.getExtra("subscribersPage");
                            Integer collectionIndex = (Integer) lastRequest.getExtra("collectionIndex");

                            int nextPage = subscribersPage + 1;
                            // add NextPage 标志: 返回结果 < 某个长度就已表示失败! or other conditions
                            if (page.isDownloadSuccess() && page.getRawText().length() > 10) {
                                saveUser(collectionIndex, page.getRawText());

                                Request request = new Request();
                                String nextUrl = String.format(userBaseUrl, collectionIndex, nextPage, queryCount);
                                request.setUrl(nextUrl);
                                request.putExtra("subscribersPage",subscribersPage + 1);
                                request.putExtra("collectionIndex",collectionIndex);
                                request.setPriority(0);
                                page.addTargetRequest(request);
                            }
                        }
                    }
                })
                .thread(20)
                .downloader(downloader)
                .scheduler(new BDBScheduler())
                // 只有设置此项true: 错误的Page才会返回,否则默认重试-> 指定次数
                .errorReturn(true)
                .sleepTime(0)
                .retrySleepTime(0)
                .urls("https://api.jianshu.io/v2/collections/77")
                .run();
    }

    private static void configDB() {
        DBHelper.setType(DBHelper.TYPE_MYSQL);
        DBHelper.setDBName("jianshu_special");

        // dao config
        jianSubscriberDao = new JianSubscriberDao();
        jianSpecialDao = new JianSpecialDao();

        jianSpecialDao.createTable();
        jianSubscriberDao.createTable();
    }

    private static void saveSpecial(Integer collectionIndex, String rawText) {
        Gson gson = new Gson();
        JianSpecial jianSpecial = gson.fromJson(rawText, JianSpecial.class);
        jianSpecial.setJsonText(rawText);
        jianSpecialDao.save(jianSpecial);
    }

    private static void saveUser(Integer collectionIndex, String rawText) {
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<JianSubscriber>>() {}.getType();
        List<JianSubscriber> results = gson.fromJson(rawText, type);

        for (JianSubscriber subscriber : results) {
            subscriber.setSpecialId(collectionIndex + "");
            jianSubscriberDao.save(subscriber);
        }
    }
}