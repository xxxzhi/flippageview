package com.example.testflippageview;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		FlipVerticalPageLayout page = (FlipVerticalPageLayout)findViewById(R.id.flip);
		TextView next = new TextView(this);
		next.setText("不保存");
		page.setNextPageBack(next);
		
		TextView pre = new TextView(this);
		next.setText("保存");
		page.setNextPageBack(next);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
