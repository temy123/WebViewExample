package kr.test.webviewtestexample;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity {
    private WebView mWebview;
    private FrameLayout mLayoutBackground;
    private ProgressBar mProgressBar;
    private LinearLayout mLayoutParent;

    //    private static final String STR_URL = "http://www.javascripter.net/faq/windowclosed.htm";
    private static final String STR_URL = "https://lachy.id.au/dev/2005/11/window-open-close-test.html";
//    private static final String STR_URL = "http://naver.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup View
        setupViews();
        // 웹뷰 호출
        initWebView();
    }

    @Override
    public void onBackPressed() {
        // newWebView 가 추가되었을 경우 먼저 처리
        if (mLayoutBackground.getChildCount() > 2) {

            WebView newWebView = (WebView) mLayoutBackground.getChildAt(mLayoutBackground.getChildCount() - 2);
            if (newWebView.canGoBack()) {
                newWebView.goBack();
            } else {
                // 삭제 하기전에 기록 삭제
                newWebView.loadUrl("about:blank");
                newWebView.destroy();
                mLayoutBackground.removeView(newWebView);

                // 삭제 후 마지막 WebView 에게 onResume 이벤트
                invalidateWebView(getLatestWebView());
            }

        } else {
            if (mWebview.canGoBack()) {
                mWebview.goBack();
            } else {
                super.onBackPressed();
            }
        }
    }

    private void setupViews() {
        mLayoutBackground = findViewById(R.id.layout_background);
        mLayoutParent = findViewById(R.id.layout_parent);
        mProgressBar = findViewById(R.id.progressBar);
    }

    private void settingWebView(WebView web) {
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true); // Window.open 허용
        web.getSettings().setSupportMultipleWindows(true); // 다중창으로 뜨는 다이얼로그 허용
        web.getSettings().setSupportZoom(true); // 확대가 되지 않는 문제 강제 수동으로 허용
        web.getSettings().setBuiltInZoomControls(true); // 확대가 되지 않는 문제 강제 수동으로 허용
        web.getSettings().setDisplayZoomControls(false); // Zoom Controller 보이지 않도록
        web.getSettings().setLoadsImagesAutomatically(true);
        turnOnMixedContent(web);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            web.getSettings().setPluginState(WebSettings.PluginState.ON); // 18 이전 버전 까지 지원
        }
    }

    // MixedContent 옵션이 LOLLIPOP 이후로 자동 Disable 되어있음
    // 강제로 On 필요
    private void turnOnMixedContent(WebView web) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // Image 표시
        }
    }

    private void initWebView() {
        mWebview = new WebView(this);
        mWebview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mWebview.loadUrl(STR_URL);
        mWebview.setWebChromeClient(defaultWebChromClient());
        mWebview.setWebViewClient(defaultWebViewClient());
        settingWebView(mWebview);

        mLayoutBackground.addView(mWebview, mLayoutBackground.getChildCount() - 1);
    }

    private WebViewClient defaultWebViewClient() {
        return new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);

//                invalidateWebView(getLatestWebView());
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                switch (error.getPrimaryError()) {
                    case SslError.SSL_EXPIRED:
                    case SslError.SSL_IDMISMATCH:
                    case SslError.SSL_NOTYETVALID:
                    case SslError.SSL_UNTRUSTED:
                    case SslError.SSL_DATE_INVALID:
                    case SslError.SSL_INVALID:
                        showSSLErrorDialog(MainActivity.this, handler);
                        break;
                    default:
                        handler.proceed();
                        break;
                }
            }
        };
    }

    /**
     * 인증서 관련 팝업창으로 사용자 동의 구하기
     *
     * @param context
     * @param handler
     */
    private void showSSLErrorDialog(Context context, final SslErrorHandler handler) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("인증서");
        builder.setMessage("문제가 있을 수 있는 SSL인증서입니다\n해당 인증서를 계속해서 허용하시겠습니까?");
        builder.setPositiveButton("승인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                handler.proceed();
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                handler.cancel();
            }
        });
        builder.show();
    }

    private WebChromeClient defaultWebChromClient() {
        return new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgressBar.setProgress(newProgress);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(message);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
                builder.setCancelable(false); // confirm 을 위해 외부 닫기는 비허용
                builder.show();
                return true;
            }

            @Override
            public void onCloseWindow(WebView window) {
                // close 시 WebView 삭제
                getLatestWebView().loadUrl("about:blank");
                getLatestWebView().destroy();
                mLayoutBackground.removeView(getLatestWebView());
                // 삭제 후 마지막 WebView 에게 onResume 이벤트
                invalidateWebView(getLatestWebView());
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // Webview 내에 다른 Window 삭제
                WebView newWebView = addNewWebView();

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();

                return true;
            }
        };
    }

    private WebView addNewWebView() {
        // 새로 표시할 Page 를 위해 WebView 생성
        WebView newWebView = new WebView(this);
        newWebView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newWebView.setWebViewClient(defaultWebViewClient());
        newWebView.setWebChromeClient(defaultWebChromClient());
        settingWebView(newWebView);
        // newWebView 추가
        mLayoutBackground.addView(newWebView, mLayoutBackground.getChildCount() - 1);
        return newWebView;
    }


    private WebView getLatestWebView() {
        WebView webView = null;

        if (mLayoutBackground.getChildCount() > 2) {
            webView = (WebView) mLayoutBackground.getChildAt(mLayoutBackground.getChildCount() - 2);
        } else {
            webView = mWebview;
        }

        return webView;
    }


    private void invalidateWebView(WebView webView) {
        webView.onResume();
        webView.postInvalidate();
    }


}
