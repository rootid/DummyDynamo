package com.ub.buffalo;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 
 * @author vikram
 *
 */
public class MainActivity extends Activity  implements OnClickListener{
	private static final String MAIN_ACTIVITY = "Main activity";
	/** Called when the activity is first created. */
	private Button put1Button;
	private Button put2Button;
	private Button put3Button;
	private Button getButton;
	private Button dumpButton;
	public static TextView textView;
	private EditText editText;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	public static Handler uiListenHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			textView.append( (String)msg.obj);
		}
	};

	@Override
	protected void onResume() {
		super.onResume();		
		put1Button = (Button)findViewById(R.id.put1);
		put2Button = (Button)findViewById(R.id.put2);
		put3Button = (Button)findViewById(R.id.put3);
		dumpButton = (Button)findViewById(R.id.dump);
		getButton = (Button) findViewById(R.id.get);
		textView = (TextView) findViewById(R.id.text_view);
		editText = (EditText) findViewById(R.id.edit_text);
		put1Button.setOnClickListener(this);
		put1Button.setOnClickListener(this);
		put2Button.setOnClickListener(this);
		put3Button.setOnClickListener(this);
		dumpButton.setOnClickListener(this);
		getButton.setOnClickListener(this);
		textView.setOnClickListener(this);
		editText.setOnClickListener(this);
		ScrollView sView = (ScrollView)findViewById(R.id.ScrollView01);
		sView.setVerticalScrollBarEnabled(true);
		sView.scrollTo(0,  0);
		//TODO :remove deleteData after
		deleteData();

		if(!MyContentProvider.recoveryMap.isEmpty()) {
			textView.setText("Recovering ....");
			MyContentProvider.performRecovery();
		}
	}

	private void deleteData() {

		//Get the Resolver
		ContentResolver resolver = MainActivity.this.getContentResolver();

		//Make the invocation. # of rows deleted will be sent back
		int rows = resolver.delete(MyContentProvider.contentUri, null, null);

		Log.d(MAIN_ACTIVITY,"rows deleted :"+rows);

	}

	public void onClick(View v) {

		Uri uri;
		textView.setText("");
		switch (v.getId()) {

		case R.id.put1:			
			//insert 1-10	
			MyContentProvider.INSERT = 10;
			getContentResolver().update(MyContentProvider.contentUri, 
					null, null, null);
			break;
		case R.id.put2:	
			//insert 10-20
			MyContentProvider.INSERT = 20;
			getContentResolver().update(MyContentProvider.contentUri, 
					null, null, null);
			break;
		case R.id.put3:	
			//insert 20-30
			MyContentProvider.INSERT = 30;
			getContentResolver().update(MyContentProvider.contentUri, 
					null, null, null);
			break;
		case R.id.get:		
			//query
			textView.setText("Querying All....");
			getContentResolver().query(MyContentProvider.contentUri,new String []{
					ProviderMetaData.ProviderTable.PROVIDER_KEY,
					ProviderMetaData.ProviderTable.PROVIDER_VALUE
			},null, null, Util.DISTRIBUTED_GET);
			break;
		case R.id.dump:
			//query self
			textView.setText("Querying self .......");
			getContentResolver().query(MyContentProvider.contentUri,new String []{
					ProviderMetaData.ProviderTable.PROVIDER_KEY,
					ProviderMetaData.ProviderTable.PROVIDER_VALUE
			},null, null, Util.ORDER);

			if(textView.getText().toString().equals("")) {
				textView.setText("No data inserted in this node \n");
			}
			break;

		default:
			break;
		}

	}


}