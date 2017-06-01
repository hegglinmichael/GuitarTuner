package com.deviantdev.libpd;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends ActionBarActivity {

	private PdUiDispatcher dispatcher;

	Button btnPlaySound;
	ToggleButton btnToggleSound;
	SeekBar seekbarFrequency;
	SeekBar seekbarVolume;

	int volume = 0; //0 to 100

	/**
	 * Initialize the PureData Library and prepare the audio output.
	 * @throws IOException
	 */
	private void init_pd() throws IOException {
		// Configure the audio glue
		int sampleRate = AudioParameters.suggestSampleRate();
		PdAudio.initAudio( sampleRate, 0, 2, 8, true );

		// Create and install the dispatcher
		dispatcher = new PdUiDispatcher();
		PdBase.setReceiver( dispatcher );
	}

	/**
	 * Load the prepared PureData patch from the resources (raw).
	 * @throws IOException
	 */
	private void load_pd_patch() throws IOException {
		File dir = getFilesDir();
		IoUtils.extractZipResource( getResources().openRawResource( R.raw.pdpatch ), dir, true );
		File patchFile = new File( dir, "pdpatch.pd" );
		PdBase.openPatch( patchFile.getAbsolutePath() );
	}

	/**
	 * Initialize the PureData patch with default values.
	 */
	private void init_pd_patch() {

		PdBase.sendFloat( "osc_pitch", 0f ); //set base frequency
		PdBase.sendFloat( "osc_volume", 0f ); //full volume for the beginning

		PdAudio.startAudio( this );
	}

	/**
	 * Initialize the GUI elements and necessary handlers.
	 */
	private void init_gui() {

		// touch to play button
		this.btnPlaySound = (Button) findViewById( R.id.btnPlaySound );
		this.btnPlaySound.setOnTouchListener( new View.OnTouchListener() {
			@Override
			public boolean onTouch( View v, MotionEvent event ) {
				if ( event.getAction() == MotionEvent.ACTION_DOWN ) {
					PdBase.sendFloat( "osc_volume", volume / 100f ); // send volume (0 to 1)
				} else if ( event.getAction() == MotionEvent.ACTION_UP ) {
					PdBase.sendFloat( "osc_volume", 0 ); // quiet down
				}
				return false;
			}
		} );

		// toggle play button
		this.btnToggleSound = (ToggleButton) findViewById( R.id.btnToggleSound );
		this.btnToggleSound.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {

				if ( btnPlaySound.isEnabled() ) {
					btnPlaySound.setEnabled( false );
					PdBase.sendFloat( "osc_volume", volume / 100f ); // enable volume while locked
				} else {
					btnPlaySound.setEnabled( true );
					PdBase.sendFloat( "osc_volume", 0 ); // quiet down for after unlock
				}
			}
		} );

		// seekbar for frequency
		this.seekbarFrequency = (SeekBar) findViewById( R.id.seekbarFrequency );
		this.seekbarFrequency.setMax( 100 );
		this.seekbarFrequency.incrementProgressBy( 1 );
		this.seekbarFrequency.setProgress( 0 );
		this.seekbarFrequency.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
			TextView text = (TextView) findViewById( R.id.textSeekbarFrequency );

			public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
				if ( progress == 0 ) progress = 1;
				float a = progress / 100f;
				float frequency = (float) ( 2500 * Math.exp( 2.19722 * a ) - 2500 );
				text.setText( (int) frequency + " Hz" );
				PdBase.sendFloat( "osc_pitch", frequency );
			}

			public void onStartTrackingTouch( SeekBar seekBar ) {}

			public void onStopTrackingTouch( SeekBar seekBar ) {}
		} );

		// seekbar for volume
		this.seekbarVolume = (SeekBar) findViewById( R.id.seekbarVolume );
		this.seekbarVolume.setMax( 100 );
		this.seekbarVolume.incrementProgressBy( 1 );
		this.seekbarVolume.setProgress( 0 );
		this.seekbarVolume.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
				volume = progress;
				if ( btnToggleSound.isChecked() ) {
					PdBase.sendFloat( "osc_volume", volume / 100f ); // send volume (0 to 1) if locked
				}
			}

			public void onStartTrackingTouch( SeekBar seekBar ) {}

			public void onStopTrackingTouch( SeekBar seekBar ) {}
		} );
	}

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		try {
			init_pd();
			load_pd_patch();
		} catch ( IOException e ) {
			finish();
		}

		this.init_gui();
		this.init_pd_patch();
	}

	@Override
	protected void onResume() {
		super.onResume();
		PdAudio.startAudio(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		PdAudio.stopAudio();
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		getMenuInflater().inflate( R.menu.menu_main, menu );
		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		int id = item.getItemId();

		if ( id == R.id.action_link ) {
			Intent browserIntent = new Intent( Intent.ACTION_VIEW, Uri.parse( "http://www.journal.deviantdev.com/example-libpd-android-studio/" ) );
			startActivity( browserIntent );
			return true;
		}

		if ( id == R.id.action_exit ) {
			moveTaskToBack( true );
			return true;
		}

		return super.onOptionsItemSelected( item );
	}
}
