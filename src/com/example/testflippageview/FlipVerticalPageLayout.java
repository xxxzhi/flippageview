
/**
 * 构建两层mianLayout ，invisibleLayout
 * eBook继承FrameLayout，好处在于FrameLayout有图层效果，后添加的View类能覆盖前面的View。
 * 初始化：定义一个LinearLayout的成员变量mView，将page.xml inflate 成View分别用leftPage，rightPage引用，
 * 并初始化其数据，将leftPage，rightPage通过addView添加到mView，然后将mView添加到eBook。
 * 在eBook里定义一个私有类BookView extends View。 并定义成员变量 BookView mBookView；
 *  最后将mBookView添加的eBook中，这样，mView中的内容为书面内容，mBookView中的内容为特效内容。
 */
package com.example.testflippageview;
import java.util.Date;
import java.util.List;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class FlipVerticalPageLayout extends FrameLayout {
	public static final String TAG = "CrawlLayout";
	
	/**
	 * 
	 * @author houzhi
	 *	
	 */
	enum BookState {
		ABOUT_TO_ANIMATE, ANIMATING, ANIMATE_END, READY, TRACKING
	}
	/**
	 * 点击翻页位置
	 * @author houzhi
	 *
	 */
	enum Corner {
		LeftTop, RightTop, LeftBottom, RightBottom, None
	}
	/**
	 * 监听翻页
	 * @author houzhi
	 *
	 */
	public interface OnCrawlListener{
		
		/**
		 * 向左，也就是prev 翻页
		 */
		public void onCrawlLeftListener();
		
		/**
		 * 向右，也就是next 翻页
		 */
		public void onCrawlRightListener();
		 
		/**
		 * 动画结束时将会被调用
		 */
		public void onCrawlEndListener();
	}
	private OnCrawlListener mOnCrawlListener = null;
	/**
	 * @param monCrawlListener the monCrawlListener to set
	 */
	public void setOnCrawlListener(OnCrawlListener mOnCrawlListener) {
		this.mOnCrawlListener = mOnCrawlListener;
	}
	
    protected View findViewTraversal(int id) {
    	Log.i(TAG,"my findViewTraversal");
    	if (id == getId()) {
            return this;
        }
    	return concrete.findViewById(id);
    }
	
	private Context mContext;
	private boolean hasInit = false;
	private final int defaultWidth = 600, defaultHeight = 400;
	/**
	 * 整个布局的宽度
	 */
	private int contentWidth = 0;
	/**
	 * 整个布局的高度
	 */
	private int contentHeight = 0;
	/**
	 * 当前页，下一页，前一页
	 */
	private View middlePage = null, nextPage = null, prevPage = null;
	private LinearLayout invisibleLayout;
	
	
	private FrameLayout mainLayout;
	
	// 包含的内容 相当于一个代理
	private FrameLayout concrete;
	private BookView mBookView;
	private Handler aniEndHandle;
	private static boolean closeBook = false;

	private Corner mSelectCorner;
	private final int clickCornerLen = 250 * 250; // 50dip
	private float scrollX = 0, scrollY = 0;

	private BookState mState;
	
	private Point aniStartPos;
	private Point aniStopPos;
	private Date aniStartTime;
	private long aniTime = 800;
	private long timeOffset = 10;

	// private Listener mListener;

	private GestureDetector mGestureDetector;
	private BookOnGestureListener mGestureListener;

	public FlipVerticalPageLayout(Context context) {
		super(context);
		Init(context);
	}

	public FlipVerticalPageLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		Init(context);
	}

	public FlipVerticalPageLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		Init(context);
	}

	private void Init(Context context) {
		Log.d(TAG, "Init");
		mContext = context;
		mSelectCorner = Corner.None;

		mGestureListener = new BookOnGestureListener();
		mGestureDetector = new GestureDetector(context,mGestureListener);
		mGestureDetector.setIsLongpressEnabled(false);
		aniEndHandle = new Handler();

		concrete = new FrameLayout(mContext);
		concrete.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		super.setOnTouchListener(touchListener);
		this.setLongClickable(true);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		Log.d(TAG, "dispatchDraw");
		super.dispatchDraw(canvas);

		if (!hasInit) {
			hasInit = true;
			mState = BookState.READY;

			mainLayout = new FrameLayout(mContext);
			mainLayout.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));
			mainLayout.setBackgroundColor(0xffffffff);
			invisibleLayout = new LinearLayout(mContext);
			invisibleLayout.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));

			this.addView(invisibleLayout);
			this.addView(mainLayout);

			mBookView = new BookView(mContext);
			mBookView.setLayoutParams(new LayoutParams(contentWidth,
					contentHeight));
			// mBookView.setBackgroundColor(Color.argb(255, 255, 0, 0));
			this.addView(mBookView);

			updatePageView();
			invalidate();
		} else if (mState == BookState.READY) {
			mBookView.update();
		}

	}

	boolean hasFlip = false;

	private void updatePageView() {
		Log.d(TAG, "updatePageView");
		invisibleLayout.removeAllViews();

		if (hasFlip) {
			removeAllViews();
			setVisibility(View.GONE);
			if(mOnCrawlListener!=null){
				Log.i(TAG,"mOnCralListener");
				mOnCrawlListener.onCrawlEndListener();
			}
			System.out.println("hasflip");
			return;
		}
		hasFlip = true;
		/* 当前页 */
		mainLayout.addView(concrete);
		/* 背景页 */
		middlePage = new WhiteView(mContext);
		// middlePage = new ImageView(mContext);
		// middlePage.setBackgroundResource(R.drawable.temp);
		middlePage
				.setLayoutParams(new LayoutParams(contentWidth, contentHeight));
		invisibleLayout.addView(middlePage);

		/* 前一页 */
		if (prevPage == null)
			prevPage = new WhiteView(mContext);
		prevPage.setLayoutParams(new LayoutParams(contentWidth, contentHeight));
		invisibleLayout.addView(prevPage);

		/* 后一页 */
		if (nextPage == null)
			nextPage = new WhiteView(mContext);
		// nextPage = new ImageView(mContext);
		// nextPage.setBackgroundResource(R.drawable.temp);
		nextPage.setLayoutParams(new LayoutParams(contentWidth, contentHeight));
		invisibleLayout.addView(nextPage);

		Log.d(TAG, "updatePageView finish");
	}

	/**
	 * 设置向前翻页 的背景 即点击点在左边
	 * @param v
	 */
	public void setPrevPageBack(View v){
		prevPage = v;
	}
	
	/**
	 * 设置向后翻页 的背景 即点击点在右边
	 * @param v
	 */
	public void setNextPageBack(View v){
		prevPage = v;
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		System.out.println("dispatch");
		hasTouch = false;
		boolean ret = super.dispatchTouchEvent(ev);
		return ret;
	}
	boolean hasTouch = false;
	OnTouchListener touchListener = new OnTouchListener() {
		
		public boolean onTouch(View v, MotionEvent event) {
			hasTouch = true;
			boolean ret = false;
			if (mTouchListener != null)
				ret = mTouchListener.onTouch(v, event);
			Log.d(TAG,
					"onTouch " + " x: " + event.getX() + " y: " + event.getY()
							+ " mState:" + mState);
			mGestureDetector.onTouchEvent(event);
			int action = event.getAction();
			if (action == MotionEvent.ACTION_UP && mSelectCorner != Corner.None
					&& mState == BookState.TRACKING) {
				if (mState == BookState.ANIMATING)
					return false;
				if (mSelectCorner == Corner.LeftTop) {
					if (scrollY < contentHeight / 2) {
						aniStopPos = new Point(0, 0);
					} else {
						//因为计算的时候分界线是一半，所以这里取的是两倍。才能到达全部翻页完
						aniStopPos = new Point(0, 2*contentHeight);
					}
				} else if (mSelectCorner == Corner.RightTop) {
					if (scrollY < contentHeight / 2) {
						aniStopPos = new Point(contentWidth, 0);
					} else {
						aniStopPos = new Point(contentWidth, 2*contentHeight);
					}
				} else if (mSelectCorner == Corner.LeftBottom) {
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(0, 0);
					} else {
						aniStopPos = new Point(0, contentHeight);
					}
				} else if (mSelectCorner == Corner.RightBottom) {
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(contentWidth, -contentHeight);
					} else {
						aniStopPos = new Point(contentWidth, contentHeight);
					}
				}
				aniStartPos = new Point((int) scrollX, (int) scrollY);
				aniTime = 800;
				mState = BookState.ABOUT_TO_ANIMATE;
				closeBook = true;
				aniStartTime = new Date();
				Log.d(TAG, "up startAnimation");
				mBookView.startAnimation();
			}

			return ret;
		}
	};
	
	class BookOnGestureListener implements OnGestureListener {
		public boolean onDown(MotionEvent event) {
			Log.d(TAG, "onDown");
			if (mState == BookState.ANIMATING)
				return false;
			float x = event.getX(), y = event.getY();
			int w = contentWidth, h = contentHeight;
			if (x * x + y * y < clickCornerLen) {
				mSelectCorner = Corner.LeftTop;
				aniStartPos = new Point(0, 0);
			} else if ((x - w) * (x - w) + y * y < clickCornerLen) {
				mSelectCorner = Corner.RightTop;
				aniStartPos = new Point(contentWidth, 0);
			} else if (x * x + (y - h) * (y - h) < clickCornerLen) {
				mSelectCorner = Corner.LeftBottom;
				aniStartPos = new Point(0, contentHeight);
			} else if ((x - w) * (x - w) + (y - h) * (y - h) < clickCornerLen) {
				mSelectCorner = Corner.RightBottom;
				aniStartPos = new Point(contentWidth, contentHeight);
			}
			if (mSelectCorner != Corner.None) {
				aniStopPos = new Point((int) x, (int) y);
				aniTime = 800;
				mState = BookState.ABOUT_TO_ANIMATE;
				closeBook = false;
				aniStartTime = new Date();
				mBookView.startAnimation();
			}
			Log.i("ondown", "ondown");
			return false;
		}

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			Log.d(TAG, "onFling velocityX:" + velocityX + " velocityY:"
					+ velocityY);
			if (mSelectCorner != Corner.None) {
				if (mSelectCorner == Corner.LeftTop) {
					if (velocityY < 0) {
						aniStopPos = new Point(0, 0);
					} else {
						//因为计算的时候分界线是一半，所以这里取的是两倍。才能到达全部翻页完
						aniStopPos = new Point(0, 2*contentHeight);
					}
				} else if (mSelectCorner == Corner.RightTop) {
					if (velocityY < 0) {//回滚
						aniStopPos = new Point(contentWidth, 0);
					} else {
						aniStopPos = new Point(contentWidth, 2*contentHeight);
					}
				} else if (mSelectCorner == Corner.LeftBottom) {
					if (velocityY < 0) { //翻页
						aniStopPos = new Point(0, 0);
					} else {
						aniStopPos = new Point(0, contentHeight);
					}
				} else if (mSelectCorner == Corner.RightBottom) {
					if (velocityY < 0) {//翻页
						aniStopPos = new Point(contentWidth, -contentHeight);
					} else {
						aniStopPos = new Point(contentWidth, contentHeight);
					}
				}
				Log.d(TAG, "onFling animate");
				aniStartPos = new Point((int) scrollX, (int) scrollY);
				aniTime = 1000;
				mState = BookState.ABOUT_TO_ANIMATE;
				closeBook = true;
				aniStartTime = new Date();
				mBookView.startAnimation();
			}
			return false;
		}

		public void onLongPress(MotionEvent e) {
			Log.d(TAG, "onLongPress");
		}

		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			mState = BookState.TRACKING;
			if (mSelectCorner != Corner.None) {
				scrollX = e2.getX();
				scrollY = e2.getY();
				Log.d(TAG,"scroll animation");
				Log.d(TAG,"scrollx:"+scrollX+","+" scrolly:"+scrollY);
				mBookView.startAnimation();
			}
			return false;
		}

		public void onShowPress(MotionEvent e) {
			Log.d(TAG, "onShowPress");
		}

		public boolean onSingleTapUp(MotionEvent e) {
			Log.d(TAG, "onSingleTapUp");

			if (mSelectCorner != Corner.None) {
				if (mSelectCorner == Corner.LeftTop) {
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(0, 0);
					} else {
						//因为计算的时候分界线是一半，所以这里取的是两倍。才能到达全部翻页完
						aniStopPos = new Point(0, 2*contentHeight);
					}
				} else if (mSelectCorner == Corner.RightTop) {
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(contentWidth, 0);
					} else {
						aniStopPos = new Point(contentWidth, 2*contentHeight);
					}
				} else if (mSelectCorner == Corner.LeftBottom) {
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(0, 0);
					} else {
						aniStopPos = new Point(0, contentHeight);
					}
				} else if (mSelectCorner == Corner.RightBottom) {
					if (scrollX < contentWidth / 2) {
						aniStopPos = new Point(contentWidth, -contentHeight);
					} else {
						aniStopPos = new Point(contentWidth, contentHeight);
					}
				}
				aniStartPos = new Point((int) scrollX, (int) scrollY);
				aniTime = 800;
				mState = BookState.ABOUT_TO_ANIMATE;
				closeBook = true;
				aniStartTime = new Date();
				mBookView.startAnimation();
			}
			return false;
		}
	}

	@Override
	protected void onFinishInflate() {
		Log.d(TAG, "onFinishInflate");
		super.onFinishInflate();
	}

	
	
	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params) {
		// TODO Auto-generated method stub
		super.setLayoutParams(params);
		contentHeight = params.height;
		contentWidth = params.width;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);

		contentWidth = this.getWidth();
		contentHeight = this.getHeight();
		if (contentWidth == 0)
			contentWidth = defaultWidth;
		if (contentHeight == 0)
			contentHeight = defaultHeight;
		Log.d(TAG, "onLayout, width:" + contentWidth + " height:"
				+ contentHeight);
	}

	private OnTouchListener mTouchListener = null;

	@Override
	public void setOnTouchListener(OnTouchListener l) {
		// TODO Auto-generated method stub
		mTouchListener = l;
	}

	class BookView extends SurfaceView implements SurfaceHolder.Callback {
		// 绘制线程
		DrawThread dt;
		SurfaceHolder surfaceHolder;
		Paint mDarkPaint = new Paint();
		Paint mPaint = new Paint();

		// 临时背景图片
		Bitmap tmpBmp = Bitmap.createBitmap(contentWidth, contentHeight,
				Bitmap.Config.ARGB_8888);
		// Bitmap tmpBmp = BitmapFactory.decodeResource(getResources(),
		// R.drawable.temp);
		Canvas mCanvas = new Canvas(tmpBmp);

		Paint bmpPaint = new Paint();
		Paint ivisiblePaint = new Paint();

		public BookView(Context context) {
			super(context);
			surfaceHolder = getHolder();
			surfaceHolder.addCallback(this);

			mDarkPaint.setColor(0x88000000);
			Shader mLinearGradient = new LinearGradient(0, 0, contentWidth, 0,
					new int[] { 0x00000000, 0x33000000, 0x00000000 },
					new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.MIRROR);
			mPaint.setAntiAlias(true);
			mPaint.setShader(mLinearGradient);

			bmpPaint.setFilterBitmap(true);
			bmpPaint.setAntiAlias(true);

			ivisiblePaint.setAlpha(0);
			ivisiblePaint.setFilterBitmap(true);
			ivisiblePaint.setAntiAlias(true);
			ivisiblePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		}

		public void startAnimation() {
			if (dt == null) {
				Log.d(TAG, "startAnimation");
				dt = new DrawThread(this, getHolder());
				dt.start();
			}
		}

		public void stopAnimation() {
			Log.d(TAG, "stopAnimation");
			if (dt != null) {
				dt.flag = false;
				Thread t = dt;
				dt = null;
				t.interrupt();
			}
		}

		public void drawLT(Canvas canvas) {
			double dx = contentWidth - scrollX, dy = scrollY;
			double len = Math.sqrt(dx * dx + dy * dy);
			if (len > contentWidth) {
				scrollX = (float) (contentWidth - contentWidth * dx / len);
				scrollY = (float) (contentWidth * dy / len);
			}

			double px = scrollX;
			double py = scrollY;
			double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

			Matrix m = new Matrix();
			m.postTranslate(scrollX - contentWidth, scrollY);
			m.postRotate((float) (arc), scrollX, scrollY);

			middlePage.draw(mCanvas);

			Paint ps = new Paint();
			Shader lg1 = new LinearGradient(contentWidth, 0, contentWidth
					- (float) px, (float) py, new int[] { 0x00000000,
					0x33000000, 0x00000000 },
					new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);

			prevPage.draw(mCanvas);
			Shader lg2 = new LinearGradient(scrollX, scrollY, 0, 0, new int[] {
					0x00000000, 0x33000000, 0x00000000 }, new float[] { 0.35f,
					0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);

			arc = arc * Math.PI / 360;
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);
			double p1 = r / (2 * Math.cos(arc));
			double p2 = r / (2 * Math.sin(arc));
			Log.d(TAG, "p1: " + p1 + " p2:" + p2);
			if (arc == 0) {
				path.moveTo((float) p1, 0);
				path.lineTo(contentWidth, 0);
				path.lineTo(contentWidth, contentHeight);
				path.lineTo((float) p1, contentHeight);
				path.close();
			} else if (p2 > contentHeight || p2 < 0) {
				double p3 = (p2 - contentHeight) * Math.tan(arc);
				path.moveTo((float) p1, 0);
				path.lineTo(contentWidth, 0);
				path.lineTo(contentWidth, contentHeight);
				path.lineTo((float) p3, contentHeight);
				path.close();
			} else {
				path.moveTo((float) p1, 0);
				path.lineTo(contentWidth, 0);
				path.lineTo(contentWidth, contentHeight);
				path.lineTo(0, contentHeight);
				path.lineTo(0, (float) p2);
				path.close();
			}
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		public void drawLB(Canvas canvas) {
			double dx = contentWidth - scrollX, dy = contentHeight - scrollY;
			double len = Math.sqrt(dx * dx + dy * dy);
			if (len > contentWidth) {
				scrollX = (float) (contentWidth - contentWidth * dx / len);
				scrollY = (float) (contentHeight - contentWidth * dy / len);
			}
			double px = scrollX;
			double py = contentHeight - scrollY;
			double arc = 2 * Math.atan(py / px) * 180 / Math.PI;

			Matrix m = new Matrix();
			m.postTranslate(scrollX - contentWidth, scrollY - contentHeight);
			m.postRotate((float) (-arc), scrollX, scrollY);

			middlePage.draw(mCanvas);

			Paint ps = new Paint();
			Shader lg1 = new LinearGradient(contentWidth, contentHeight,
					contentWidth - (float) px, contentHeight - (float) py,
					new int[] { 0x00000000, 0x33000000, 0x00000000 },
					new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);

			prevPage.draw(mCanvas);
			Shader lg2 = new LinearGradient(scrollX, scrollY, 0, contentHeight,
					new int[] { 0x00000000, 0x33000000, 0x00000000 },
					new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);

			arc = arc * Math.PI / 360;
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);
			double p1 = r / (2 * Math.cos(arc));
			double p2 = r / (2 * Math.sin(arc));
			Log.d(TAG, "p1: " + p1 + " p2:" + p2);
			if (arc == 0) {
				path.moveTo((float) p1, 0);
				path.lineTo(contentWidth, 0);
				path.lineTo(contentWidth, contentHeight);
				path.lineTo((float) p1, contentHeight);
				path.close();
			} else if (p2 > contentHeight || p2 < 0) {
				double p3 = (p2 - contentHeight) * Math.tan(arc);
				path.moveTo((float) p3, 0);
				path.lineTo(contentWidth, 0);
				path.lineTo(contentWidth, contentHeight);
				path.lineTo((float) p1, contentHeight);
				path.close();
			} else {
				path.moveTo(0, 0);
				path.lineTo(contentWidth, 0);
				path.lineTo(contentWidth, contentHeight);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(0, contentHeight - (float) p2);
				path.close();
			}
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		/**
		 * 点击点在右上角
		 * @param canvas
		 */
		public void drawRT(Canvas canvas) {
			double dx = scrollX, dy = scrollY;
			double len = Math.sqrt(dx * dx + dy * dy);
			if (len > contentHeight) {			//目的似乎很大，没明白
				scrollX = (float) (contentHeight * dx / len);
				scrollY = (float) (contentHeight * dy / len);
			}
			//计算点击点距离最近顶点的x,y的距离
			double px = contentWidth - scrollX;
			double py = scrollY;
			double arc = 2 * Math.atan(px / py) * 180 / Math.PI;
			Log.d(TAG,"px:"+px+" py:"+py+" arc:"+arc);
			Matrix m = new Matrix();
			m.postTranslate(scrollX, scrollY-contentHeight);
			m.postRotate((float) (-(90-arc)), scrollX, scrollY);
			
			middlePage.draw(mCanvas);
			Paint ps = new Paint();
			Shader lg1 = new LinearGradient(0, 0,
					(float) px, (float) py,
					new int[] { 0x00000000, 0x33000000, 0x00000000 },
					new float[] { 0.35f, 0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);
			
			nextPage.draw(mCanvas);
			//阴影
			Shader lg2 = new LinearGradient( contentWidth, 0,
					contentWidth - (float) px,(float) py,  new int[] { 0x00000000, 0x33000000,
							0x00000000 }, new float[] { 0.35f, 0.5f, 0.65f },
					Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);

			arc = arc * Math.PI / 360;			//变成弧度制
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);			//顶点与触摸点的距离
			double p1 = contentWidth - r / (2 * Math.sin(arc));			//x方向的点
			double p2 = r / (2 * Math.cos(arc));						//y方向的点
			Log.d(TAG, "p1: " + p1 + " p2:" + p2+" math.sin:"+Math.sin(arc)+" r:"+r);
			Log.d(TAG,"contentWidth:"+contentWidth);
			double p3 =0;
			if (arc ==0) {
				//相当于水平或者垂直滑动
				path.moveTo(0f, contentHeight);
				path.lineTo(0f, (float)p2);
				path.lineTo(contentWidth, (float)p2);
				path.lineTo(contentWidth, contentHeight);
				path.close();
			} else if (p1 > contentWidth || p1 < 0) {
				//超出时绘制
				if(p1<0)
					p3= (-p1) * Math.tan(arc);
				else
					p3=(p1-contentWidth)*Math.tan(arc);
				path.moveTo(0f, contentHeight);
				path.lineTo( 0,(float) p3);
				path.lineTo(contentWidth, (float)p2);
				path.lineTo(contentWidth, contentHeight);
				path.close();
			} else {
				path.moveTo(0, 0);
				path.lineTo((float) p1, 0);
				path.lineTo(contentWidth, (float) p2);
				path.lineTo(contentWidth, contentHeight);
				path.lineTo(0, contentHeight);
				path.close();
			}
			Log.i(TAG,p1+","+p2+","+p3+","+scrollX+","+scrollY);
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}
		//采用贝叶斯曲线，总共有 个点
		private List<PointF> countPoint(float x,float y,Corner corner){
			PointF a,b,c,d,e,f,g,h,i,j,k;
			a=new PointF(x, y);
			float xtemp,ytemp;
			switch(corner){
			case LeftTop:
			case LeftBottom:
			case RightBottom:
				f = new PointF(contentWidth,contentHeight);
				g = new PointF((a.x+f.x)/2,(a.y+f.y)/2);
				//计算a ,f的垂直平分线
				float slope = -1/((a.y-f.y)/(a.x-f.x));
				xtemp = contentWidth ;
				ytemp = slope*(xtemp-g.x)+g.y;
				h = new PointF(xtemp,ytemp);
				ytemp = contentHeight;
				xtemp = (ytemp-g.y)/slope+g.x;
				e = new PointF(xtemp,ytemp);
				b = new PointF((a.x+e.x)/2,(a.y+e.y)/2);
				k = new PointF((a.x+h.x)/2,(a.y+h.y)/2);
				//斜率
				slope = (k.y-b.y)/(k.x-b.x);
				xtemp = contentWidth ;
				ytemp = slope*(xtemp-b.x)+b.y;
				j= new PointF(xtemp,ytemp);
				ytemp = contentHeight;
				xtemp = (ytemp-b.y)/slope+b.x;
				c = new PointF(xtemp,ytemp);
				d = new PointF(((b.x+c.x)/2+e.x)/2,((b.y+c.y)/2+e.y)/2);
				break;
			case RightTop:
				
			}
			return null;
		}
		
		public void drawRB(Canvas canvas) {
			double dx = scrollX, dy = contentHeight - scrollY;
			double len = Math.sqrt(dx * dx + dy * dy);
			if (len > contentWidth) {
				scrollX = (float) (contentWidth * dx / len);
				scrollY = (float) (contentHeight - contentWidth * dy / len);
			}

			double px = contentWidth - scrollX;
			double py = contentHeight - scrollY;
			double arc = 2 * Math.atan(px / py) * 180 / Math.PI;

			Matrix m = new Matrix();
			m.postTranslate(scrollX, scrollY - contentHeight);
			m.postRotate((float) (arc), scrollX, scrollY);

			middlePage.draw(mCanvas);

			Paint ps = new Paint();
			Shader lg1 = new LinearGradient(0, contentHeight, (float) px,
					contentHeight - (float) py, new int[] { 0x00000000,
							0x33000000, 0x00000000 }, new float[] { 0.35f,
							0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg1);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);
			canvas.drawBitmap(tmpBmp, m, bmpPaint);

			nextPage.draw(mCanvas);
			Shader lg2 = new LinearGradient(scrollX - contentWidth, scrollY,
					contentWidth, contentHeight, new int[] { 0x00000000,
							0x33000000, 0x00000000 }, new float[] { 0.35f,
							0.5f, 0.65f }, Shader.TileMode.CLAMP);
			ps.setShader(lg2);
			mCanvas.drawRect(0, 0, contentWidth, contentHeight, ps);

			arc = arc * Math.PI / 360;
			Path path = new Path();
			double r = Math.sqrt(px * px + py * py);
			double p1 = contentWidth - r / (2 * Math.sin(arc));
			double p2 = contentHeight - r / (2 * Math.cos(arc));
			Log.d(TAG, "p1: " + p1 + " p2:" + p2);
			if (arc == 0) {
				path.moveTo(0, 0);
				path.lineTo(0f, (float)p2);
				path.lineTo(contentWidth, (float)p2);
				path.lineTo(contentWidth, 0f);
				path.close();
			} else if (p1 <0 || p1>contentWidth) {
				double p3 = contentHeight - (-p1 ) * Math.tan(arc);
				path.moveTo(0, 0);
				path.lineTo(0, (float)p3);
				path.lineTo(contentWidth, (float)p2);
				path.lineTo(contentWidth,0);
				path.close();
			} else {
				path.moveTo(0, 0);
				path.lineTo(0, contentHeight);
				path.lineTo((float) p1, contentHeight);
				path.lineTo(contentWidth, (float)p2);
				path.lineTo(contentWidth,0);
				path.close();
			}
			mCanvas.drawPath(path, ivisiblePaint);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		public void drawPrevPageEnd(Canvas canvas) {
			prevPage.draw(mCanvas);
			canvas.drawBitmap(tmpBmp, 0, 0, null);
		}

		public void drawNextPageEnd(Canvas canvas) {
			nextPage.draw(mCanvas);
			canvas.drawBitmap(tmpBmp, contentWidth, 0, null);
		}

		public void drawPage(Canvas canvas) {
			if (mSelectCorner == Corner.LeftTop) {
				Log.d(TAG, "click left top");
				drawLT(canvas);
			} else if (mSelectCorner == Corner.LeftBottom) {
				Log.d(TAG, "click left bottom");
				drawLB(canvas);
			} else if (mSelectCorner == Corner.RightTop) {
				Log.d(TAG, "click right top");
				drawRT(canvas);
			} else if (mSelectCorner == Corner.RightBottom) {
				Log.d(TAG, "click right bottom");
				drawRB(canvas);
			}
		}

		public void update() {
			Canvas canvas = surfaceHolder.lockCanvas(null);
			try {
				synchronized (surfaceHolder) {
					doDraw(canvas);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (canvas != null) {
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		}

		private void doDraw(Canvas canvas) {
			Log.d(TAG, "bookView doDraw");
			mainLayout.draw(canvas);
			// draw(canvas);
		}
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {

		}
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			update();
		}
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (dt != null) {
				dt.flag = false;
				dt = null;
			}
			Log.i(TAG,"sufaceDestroyed");
		}
	}

	/**
	 * 获得运行时的动画，scroll 状态数据
	 * 
	 * @return
	 */
	private boolean getAnimateData() {
		Log.d(TAG, "getAnimateData");
		long time = aniTime;
		Date date = new Date();
		long t = date.getTime() - aniStartTime.getTime();
		t += timeOffset;
		if (t < 0 || t > time) {
			mState = BookState.ANIMATE_END;
			return false;
		} else {
			mState = BookState.ANIMATING;
			double sx = aniStopPos.x - aniStartPos.x;
			scrollX = (float) (sx * t / time + aniStartPos.x);
			double sy = aniStopPos.y - aniStartPos.y;
			scrollY = (float) (sy * t / time + aniStartPos.y);
			return true;
		}
	}

	private void handleAniEnd(Canvas canvas) {
		Log.d(TAG, "handleAniEnd");
		if (closeBook) {
			closeBook = false;
			if (mSelectCorner == Corner.LeftTop
					|| mSelectCorner == Corner.LeftBottom) {
				if (scrollX > contentWidth / 2) {
					mBookView.drawPrevPageEnd(canvas);
					aniEndHandle.post(new Runnable() {
						public void run() {
							updatePageView();
						}
					});
				} else {
					mBookView.doDraw(canvas);
				}
			} else if (mSelectCorner == Corner.RightTop
					|| mSelectCorner == Corner.RightBottom) {
				if (scrollX < contentWidth / 2) {
					mBookView.drawNextPageEnd(canvas);
					aniEndHandle.post(new Runnable() {
						public void run() {
							updatePageView();
						}
					});
				} else {
					mBookView.doDraw(canvas);
				}
			}
			mSelectCorner = Corner.None;
			mState = BookState.READY;
		} else {
			mState = BookState.TRACKING;
		}
		mBookView.stopAnimation();
		
	}

	public class DrawThread extends Thread {
		// show view
		BookView bv;
		SurfaceHolder surfaceHolder;
		boolean flag = false;
		int sleepSpan = 30;

		public DrawThread(BookView bv, SurfaceHolder surfaceHolder) {
			this.bv = bv;
			this.surfaceHolder = surfaceHolder;
			this.flag = true;
		}

		public void run() {
			Canvas canvas = null;
			while (flag) {
				try {
					canvas = surfaceHolder.lockCanvas(null);
					if (canvas == null)
						continue;
					synchronized (surfaceHolder) {
						if (mState == BookState.ABOUT_TO_ANIMATE
								|| mState == BookState.ANIMATING) {
							bv.doDraw(canvas);
							getAnimateData();
							bv.drawPage(canvas);
						} else if (mState == BookState.TRACKING) {
							bv.doDraw(canvas);
							bv.drawPage(canvas);
						} else if (mState == BookState.ANIMATE_END) {
							handleAniEnd(canvas);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (canvas != null) {
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
				try {
					Thread.sleep(sleepSpan);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		System.out
				.println("addView(View child, int index, LayoutParams params)");
		if (child == mBookView || child == invisibleLayout
				|| child == mainLayout){
			putChildToAgency(concrete);
			super.addView(child, index, params);
		}
		else{
			System.out.println("agency");
			concrete.addView(child, index, params);
		}
	}
	
	//把子视图添加到代理视图里面
	private void putChildToAgency(ViewGroup agency){
		View child ; 
		for(int i = 0;i<getChildCount();++i){
			child = getChildAt(i);
			if(child!=null&&child!=mBookView&&child!=invisibleLayout
					 &&child != mainLayout){
				agency.addView(child, i, child.getLayoutParams());
				System.out.println("add to agency ok");
			}
		}
		for(int i = 0;i<getChildCount();++i){
			child = getChildAt(i);
			if(child!=null&&child!=mBookView&&child!=invisibleLayout
					 &&child != mainLayout){
				removeViewAt(i);
				System.out.println("remove from parent ok");
			}
		}		
	}
}
