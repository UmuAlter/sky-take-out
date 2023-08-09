package com.sky.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.properties.WeChatProperties;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 微信支付工具类
 */
@Component
public class WeChatPayUtil {

    //微信支付下单接口地址
    public static final String JSAPI = "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi";

    //申请退款接口地址
    public static final String REFUNDS = "https://api.mch.weixin.qq.com/v3/refund/domestic/refunds";

    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 获取调用微信接口的客户端工具对象
     *
     * @return
     */
    private CloseableHttpClient getClient() {
        PrivateKey merchantPrivateKey = null;
        try {
            //merchantPrivateKey商户API私钥，如何加载商户API私钥请看常见问题
            merchantPrivateKey = PemUtil.loadPrivateKey(
                    new FileInputStream(new File(weChatProperties.getPrivateKeyFilePath()))
            );
            //加载平台证书文件
            X509Certificate x509Certificate = PemUtil.loadCertificate(
                    new FileInputStream(new File(weChatProperties.getWeChatPayCertFilePath()))
            );
            //wechatPayCertificates微信支付平台证书列表。
            //你也可以使用后面章节提到的“定时更新平台证书功能”，而不需要关心平台证书的来龙去脉
            List<X509Certificate> wechatPayCertificates = Arrays.asList(x509Certificate);

            WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                    .withMerchant(
                            weChatProperties.getMchid(),
                            weChatProperties.getMchSerialNo(),
                            merchantPrivateKey)
                    .withWechatPay(wechatPayCertificates);

            // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签
            CloseableHttpClient httpClient = builder.build();
            return httpClient;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 发送post方式请求
     *
     * @param url
     * @param body
     * @return
     */
    private String post(String url, String body) throws Exception {
        // getClient() 获取调用微信接口的客户端工具对象
        CloseableHttpClient httpClient = getClient();

        HttpPost httpPost = new HttpPost(url);
        //表示客户端希望接收的响应内容类型为 JSON
        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
        //表示请求的内容类型为 JSON
        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        //添加自定义请求头，用于与微信支付平台进行通信时的认证和身份验证
        httpPost.addHeader("Wechatpay-Serial", weChatProperties.getMchSerialNo());  //商户API证书的证书序列号
        //设置请求体内容为body，并指定编码格式为 UTF-8
        httpPost.setEntity(new StringEntity(body, "UTF-8"));
        //发送
        CloseableHttpResponse response = httpClient.execute(httpPost);
        try {
            //获得预支付交易单
            String bodyAsString = EntityUtils.toString(response.getEntity());
            return bodyAsString;
        } finally {
            httpClient.close();
            response.close();
        }
    }

    /**
     * 发送get方式请求
     *
     * @param url
     * @return
     */
    private String get(String url) throws Exception {
        CloseableHttpClient httpClient = getClient();

        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString());
        httpGet.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        httpGet.addHeader("Wechatpay-Serial", weChatProperties.getMchSerialNo());

        CloseableHttpResponse response = httpClient.execute(httpGet);
        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            //预支付交易单
            return bodyAsString;
        } finally {
            httpClient.close();
            response.close();
        }
    }

    /**
     * jsapi下单
     *
     * @param orderNum    商户订单号
     * @param total       总金额
     * @param description 商品描述
     * @param openid      微信用户的openid
     * @return
     */
    private String jsapi(String orderNum,
                         BigDecimal total,
                         String description,
                         String openid
    ) throws Exception {
        //接口接入规范 https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_1_1.shtml
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("appid", weChatProperties.getAppid());
        jsonObject.put("mchid", weChatProperties.getMchid());  //直连商户号
        jsonObject.put("description", description);
        jsonObject.put("out_trade_no", orderNum);  //商户订单号
        jsonObject.put("notify_url", weChatProperties.getNotifyUrl()); //通知地址

        JSONObject amount = new JSONObject();
        amount.put("total", total
                //将 total 乘以 new BigDecimal(100)，将金额单位由元转换为分。这是因为微信支付接口中金额的单位是分。
                                .multiply(new BigDecimal(100))
                //调用 setScale(2, BigDecimal.ROUND_HALF_UP)
                // 方法设置精度为两位小数，并且使用四舍五入方式进行舍入。这是为了确保金额精确到小数点后两位
                                .setScale(2, BigDecimal.ROUND_HALF_UP)
                //调用 intValue() 方法将 BigDecimal 对象转换为 int 类型，并将结果放入 JSON 对象 amount 中，使用键名 "total"
                                .intValue());
        amount.put("currency", "CNY"); //货币类型

        jsonObject.put("amount", amount);

        JSONObject payer = new JSONObject();
        payer.put("openid", openid);

        jsonObject.put("payer", payer);

        String body = jsonObject.toJSONString();
        return post(JSAPI, body); //发送post方式请求，返回预支付交易单
    }

    /**
     * 小程序支付
     *
     * @param orderNum    商户订单号
     * @param total       金额，单位 元
     * @param description 商品描述
     * @param openid      微信用户的openid
     * @return
     */
    public JSONObject pay(String orderNum,//商户订单号
                          BigDecimal total, //支付金额，单位 元
                          String description, //商品描述
                          String openid //微信用户的openid
    ) throws Exception {
        //统一下单，生成预支付交易单
        String bodyAsString = jsapi(orderNum, total, description, openid);
        //解析返回结果
        JSONObject jsonObject = JSON.parseObject(bodyAsString);
        System.out.println(jsonObject);
        //微信提供的JSAPI接口的返回结果只有 prepay_id——预支付交易会话标识
        String prepayId = jsonObject.getString("prepay_id");
        if (prepayId != null) {
            //时间戳
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
            //生成随机字符串
            String nonceStr = RandomStringUtils.randomNumeric(32);

            ArrayList<Object> list = new ArrayList<>();
            list.add(weChatProperties.getAppid());
            list.add(timeStamp);
            list.add(nonceStr);
            list.add("prepay_id=" + prepayId);
            //二次签名，调起支付需要重新签名
            StringBuilder stringBuilder = new StringBuilder();
            for (Object o : list) {
                //追加到 stringBuilder 中，并在每个参数值后添加换行符 \n
                stringBuilder.append(o).append("\n");
            }
            String signMessage = stringBuilder.toString();
            byte[] message = signMessage.getBytes();
            //该方法指定了使用 SHA256withRSA 算法进行数字签名
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(
                    PemUtil
                    .loadPrivateKey(
                            //getPrivateKeyFilePath()商户私钥文件
                            new FileInputStream(new File(weChatProperties.getPrivateKeyFilePath()))
                    )
            );
            signature.update(message);
            //获得签名paySign
            String packageSign = Base64.getEncoder().encodeToString(signature.sign());

            //构造数据给微信小程序，用于调起微信支付
            JSONObject jo = new JSONObject();
            jo.put("timeStamp", timeStamp);
            jo.put("nonceStr", nonceStr);
            jo.put("package", "prepay_id=" + prepayId);
            jo.put("signType", "RSA");
            jo.put("paySign", packageSign);

            return jo;
        }
        return jsonObject;
    }

    /**
     * 申请退款
     *
     * @param outTradeNo    商户订单号
     * @param outRefundNo   商户退款单号
     * @param refund        退款金额
     * @param total         原订单金额
     * @return
     */
    public String refund(String outTradeNo, String outRefundNo, BigDecimal refund, BigDecimal total) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no", outTradeNo);
        jsonObject.put("out_refund_no", outRefundNo);

        JSONObject amount = new JSONObject();
        amount.put("refund", refund.multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP).intValue());
        amount.put("total", total.multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP).intValue());
        amount.put("currency", "CNY");

        jsonObject.put("amount", amount);
        jsonObject.put("notify_url", weChatProperties.getRefundNotifyUrl());

        String body = jsonObject.toJSONString();

        //调用申请退款接口
        return post(REFUNDS, body);
    }
}
