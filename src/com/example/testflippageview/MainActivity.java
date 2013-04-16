package com.example.testflippageview;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		FlipVerticalPageLayout flip = (FlipVerticalPageLayout) findViewById(R.id.flip);
		if(flip==null){
			Log.i("tag","flip is null");
		}
		ImageView iv = new ImageView(this);
		iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		iv.setImageResource(R.drawable.back);
		iv.setId(1);
//		final FlipPageLayout crawl = new FlipPageLayout(activity);
//		crawl.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
//				(int) activity.getResources().getDimension(R.dimen.top_window_width),
//				(int) activity.getResources().getDimension(R.dimen.top_window_width)));
//		crawl.addView(v);
//		flip.addView(iv);
		View v= flip.findViewById(R.id.imageView1);
		if(v==null){
			Log.i("tag","v is null");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
