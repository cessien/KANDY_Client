package org.OpenNI.Samples.SimpleViewer;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.OpenNI.*;
import org.OpenNI.Samples.Assistant.BitmapGenerator;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Vibrator;
import android.util.Log;



class SimpleViewer  {
	private final String TAG = getClass().getSimpleName(); 
	
	private OutArg<ScriptNode> scriptNode;
    private Context context;
    private DepthGenerator depthGen;
    private BitmapGenerator bitmapGenerator;
    
    private Bitmap bitmap;
    private DepthMetaData depthMD;

    public int width, height;
    private int[] pixels;
    boolean initialized;
    
    
	float bias = 0.5f;
	float area_thresh[] = {0.4f, 0.1f, 0.4f, 0.4f, 0.1f, 0.4f};
	int center0 = (int)(640/2 * bias);
	int center1 = 640 - center0;
	int[] intensity = new int[6];
	int sideArea = center0 * 240;
	int centerArea = (640 - 2*center0) * (240);
	int centerRank, leftRank, rightRank;
	
	static float vibFactor = 0.0f;
    
    public static final String SAMPLE_XML_FILE = "SamplesConfig.xml";    
    public SimpleViewer() {

        try {
            scriptNode = new OutArg<ScriptNode>();
            String xmlName = SimpleViewerActivity.getCurrentActivity().getFilesDir() +"/"+ SAMPLE_XML_FILE;
//            String xmlName = "/mnt/sdcard/SamplesConfig.xml";
            context = Context.createFromXmlFile(xmlName, scriptNode);
//            try {
//				context = new Context();
//				context.addLicense(new License("PrimeSense","0KOIk2JeIBYClPWVnMoRKn5cdY4="));
			depthGen = DepthGenerator.create(context);
			bitmapGenerator = new BitmapGenerator(context, false, true);
			MapOutputMode mapOutputMode;
			mapOutputMode = bitmapGenerator.getMapOutputMode();
			width  = mapOutputMode.getXRes();
            height = mapOutputMode.getYRes();
            bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        } catch (GeneralException e) {
            Log.e(TAG, e.toString());
            System.exit(1);
        }
        
        initialized = true;
    }
    
    public void init(){
    	new Thread(){ 
    		public void run() {
    			for(;;)
    				processCollision();
    		}
    	}.start();
    }
    
    public void Cleanup()
    {
    	Log.e(TAG, "Cleanup");
    	
    	scriptNode.value.dispose();
    	scriptNode = null;
    	
    	depthGen.dispose();
    	depthGen = null;
    	
    	try {
			bitmapGenerator.dispose();
		} catch (StatusException e) {
			Log.e(TAG, e.toString());
		}
    	
    	context.dispose();
    	context = null;

    	Log.e(TAG, "Cleanup Done");
    }

	void updateDepth() throws StatusException {
		context.waitAnyUpdateAll();
		bitmapGenerator.generateBitmap();
		depthMD = depthGen.getMetaData();
	}

    public Bitmap drawBitmap()
    {
    	float stat = 0;
    	int total = 0;
    	pixels = bitmapGenerator.getLastBitmap();
    	ShortBuffer buffer = null;
    	try {
			buffer = depthGen.getDepthMap().createShortBuffer();
		} catch (GeneralException e) {
			e.printStackTrace();
		}
    	
    	for(int i = 0; i < intensity.length; i++){
    		intensity[i] = 0;
    	}
    	
    	while(buffer.hasRemaining()){
    		if(buffer.get() < (short)1000 + (short)SimpleViewerActivity.variables[5]*100){
    			int index = buffer.position() - 1;
    			total++;
    			pixels[index] = 0xFFFF0000;
    			
    			int x = (index) % width;
    			int y = (index) / width;

    			if(x < center0){
    				if(y < 240)
    					intensity[0]++;
    				else
    					intensity[3]++;
    			} else if(x >= center0 && x < center1){
    				if(y < 240)
    					intensity[1]++;
    				else
    					intensity[4]++;
    			} else if(x >= center1){
    				if(y < 240)
    					intensity[2]++;
    				else
    					intensity[5]++;
    			}
    		}
    	}

//    	for(int i = 0; i < pixels.length; i++) {
//    		if((pixels[i]&0xFF) > 0xAA) {
//    			pixels[i] = 0xFFFF0000;
//    			
//    			total++;
//    			int x = i % width;
//    			int y = i / width;
//
//    			if(x < center0){
//    				if(y < 240)
//    					intensity[0]++;
//    				else
//    					intensity[3]++;
//    			} else if(x >= center0 && x < center1){
//    				if(y < 240)
//    					intensity[1]++;
//    				else
//    					intensity[4]++;
//    			} else if(x >= center1){
//    				if(y < 240)
//    					intensity[2]++;
//    				else
//    					intensity[5]++;
//    			}
//    		}
//    	}
    	
    	stat = (float) (total/(640.0f*480.0f));

    	if (stat > .7f){
    		vibFactor = 100.0f;
    	} else {
    		vibFactor = stat * 100.0f;
    	}
    	Log.d("Alert", "vibFactor: "+vibFactor);
//    	processAlerts();
    	
    	bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

		return bitmap;
    }
    
    public void processAlerts(){
    	float intens[] = new float[6];
    	intens[0] = (float)(intensity[0] / (float)sideArea);
    	intens[1] = (float)(intensity[1] / (float)centerArea);
    	intens[2] = (float)(intensity[2] / (float)sideArea);
    	intens[3] = (float)(intensity[3] / (float)sideArea);
    	intens[4] = (float)(intensity[4] / (float)centerArea);
    	intens[5] = (float)(intensity[5] / (float)sideArea);
    	
    	float norm_intens[] = new float[6];
    	norm_intens[0] = (intens[0] - area_thresh[0]) / (1 - area_thresh[0]);
    	norm_intens[1] = (intens[1] - area_thresh[1]) / (1 - area_thresh[1]);
    	norm_intens[2] = (intens[2] - area_thresh[2]) / (1 - area_thresh[2]);
    	norm_intens[3] = (intens[3] - area_thresh[3]) / (1 - area_thresh[3]);
    	norm_intens[4] = (intens[4] - area_thresh[4]) / (1 - area_thresh[4]);
    	norm_intens[5] = (intens[5] - area_thresh[5]) / (1 - area_thresh[5]);
    	
//    	Log.d("Alert", "Left Intens: " + intens[0] + ", " + intens[3]);
        //CENTER Processing
        if(intens[1] < area_thresh[1] && intens[4] < area_thresh[4]){
            //SAFE.. Nothing in center
            centerRank = 0;
        } else if (intens[1] > area_thresh[1] && intens[4] > area_thresh[4]){
            //Center: high collision factor
            centerRank = 3;
        } else if (intens[1] > area_thresh[1] && intens[4] < area_thresh[4]){
            //Area 1 (Center top): high collision factor
            centerRank = 1;
        } else if (intens[1] < area_thresh[1] && intens[4] > area_thresh[4]){
            //Area 4 (Center bot): high collision factor
            centerRank = 2;
        }

        //LEFT Processing
        if(intens[0] < area_thresh[0] && intens[3] < area_thresh[3]){
            //SAFE.. Nothing on the left
            leftRank = 0;
        } else if (intens[0] > area_thresh[0] && intens[3] > area_thresh[3]){
            //Left: high collision factor
            leftRank = 3;
        } else if (intens[0] > area_thresh[0] && intens[3] < area_thresh[3]){
            //Area 0 (Left top): high collision factor
            leftRank = 1;
        } else if (intens[0] < area_thresh[0] && intens[3] > area_thresh[3]){
            //Area 3 (Left bot): high collision factor
            leftRank = 2;
        }

        //RIGHT Processing
        if(intens[2] < area_thresh[2] && intens[5] < area_thresh[5]){
            //SAFE.. Nothing on the left
            rightRank = 0;
        } else if (intens[2] > area_thresh[2] && intens[5] > area_thresh[5]){
            //Left: high collision factor
            rightRank = 3;
        } else if (intens[2] > area_thresh[2] && intens[5] < area_thresh[5]){
            //Area 0 (Left top): high collision factor
            rightRank = 1;
        } else if (intens[2] < area_thresh[2] && intens[5] > area_thresh[5]){
            //Area 3 (Left bot): high collision factor
            rightRank = 2;
        }

        if(centerRank < 0 && leftRank < 0 && rightRank < 0){
        	vibFactor = 0.0f;
        } else if(centerRank > 0 && leftRank < 0 && rightRank < 0){
        	vibFactor = (norm_intens[1]*0.6f + norm_intens[5]*0.4f) * 100.0f;
        } else if(centerRank > 0 && leftRank < 0 && rightRank > 0){
        	vibFactor = (norm_intens[2]*0.6f + norm_intens[5]*0.4f) * 100.0f;
        } else if(centerRank > 0 && leftRank > 0 && rightRank < 0){
        	vibFactor = (norm_intens[2]*0.6f + norm_intens[5]*0.4f) * 100.0f;
        } else if(centerRank > 0 && leftRank > 0 && rightRank > 0){
        	vibFactor = (norm_intens[2]*0.6f + norm_intens[5]*0.4f) * 100.0f;
        } else if(centerRank < 0 && leftRank < 0 && rightRank > 0){
        	vibFactor = (norm_intens[2]*0.6f + norm_intens[5]*0.4f) * 100.0f;
        } else if(centerRank < 0 && leftRank > 0 && rightRank < 0){
        	vibFactor = (norm_intens[2]*0.6f + norm_intens[5]*0.4f) * 100.0f;
        } else if(centerRank < 0 && leftRank > 0 && rightRank > 0){
        	vibFactor = (norm_intens[2]*0.6f + norm_intens[5]*0.4f) * 100.0f;
        }
        
        
        //Center = C, Left = L, Right = R
        if(centerRank == 0){ //C clear
            if(leftRank == 0){ //*****CL clear*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit
                	Log.d("Alert", "Right hit");
                	vibFactor = (norm_intens[2]*0.6f + norm_intens[5]*0.4f) * 100.0f;
                	
                }
            } else if(leftRank == 1){ //*****C clear, L top*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 2){ //*****C clear, L bot*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 3){ //*****C clear, L hit*****
                if(rightRank == 0){ //R clear
                	Log.d("Alert", "Left hit");
                	vibFactor = (norm_intens[0]*0.6f + norm_intens[3]*0.4f) * 100.0f;
                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit
                	Log.d("Alert", "Left and Right hit");
                	vibFactor = (norm_intens[0]*0.25f + norm_intens[2]*0.25f + norm_intens[3]*0.25f + norm_intens[5]*0.25f) * 50.0f;
                }
            }

        } else if(centerRank == 1){ //Collision center above
            if(leftRank == 0){ //*****C top, L clear*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 1){ //*****C top, L top*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 2){ //*****C top, L bot*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 3){ //*****C top, L hit*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            }

        } else if(centerRank == 2){    //Collision center below
            if(leftRank == 0){ //*****C bot, L clear*****
                if(rightRank == 0){ //R clear
                	
                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 1){ //*****C bot, L top*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 2){ //*****C bot, L bot*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 3){ //*****C bot, L hit*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            }
        } else if(centerRank == 3){ //Many collisions center
            if(leftRank == 0){ //*****C hit, L clear*****
                if(rightRank == 0){ //R clear
                	vibFactor = (norm_intens[1]*0.7f + norm_intens[4]*0.3f) * 100.0f;
                	Log.d("Alert", "Center hit");
                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit
                	Log.d("Alert", "Center and Right hit");
                	vibFactor = (norm_intens[1]*0.5f + norm_intens[2]*0.333f + norm_intens[4]*0.333f + norm_intens[5]*0.166f) * 100.0f;
                }
            } else if(leftRank == 1){ //*****C hit, L top*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 2){ //*****C hit, L bot*****
                if(rightRank == 0){ //R clear

                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            } else if(leftRank == 3){ //*****C hit, L hit*****
                if(rightRank == 0){ //R clear
                	Log.d("Alert", "Center and Left hit");
                	vibFactor = (norm_intens[1]*0.5f + norm_intens[0]*0.333f + norm_intens[4]*0.333f + norm_intens[3]*0.166f) * 100.0f;
                } else if(rightRank == 1){ //R top

                } else if(rightRank == 2){ //R bot

                } else if(rightRank == 3){ //R hit

                }
            }
        }
        
        vibFactor = (norm_intens[0] * 0.1f + norm_intens[1] * 0.3f + norm_intens[2] * 0.1f + norm_intens[3] * 0.1f + norm_intens[4] * 0.3f + norm_intens[5] * 0.1f) * 100.0f;
        Log.d("Alert", "vibFactor: " + vibFactor);
    }
    
    public void processCollision(){
    	float depthHist[] = new float[10000];
    	int numberOfPoints = 0;
    	ShortBuffer data;
    	
		data = depthMD.getData().createShortBuffer();
    	
    	data.rewind();
    	//Create a histogram of values per frame of the depth stream. Used for analyzation
    	while(data.remaining() > 0)
        {
    		short depth = 0;
    		depth = data.get();
    		
    		if(depth != 0){
				depthHist[depth]++;
				numberOfPoints++;
			}
        }
    	
    	if(numberOfPoints != 0){
			for(int nI = 0; nI < 10000; nI++){
				depthHist[nI] = 256.000f*(1.000f - (depthHist[nI] / numberOfPoints));
			}
		}
    	
    	data.rewind();
    	//Create a histogram of values per frame of the depth stream. Used for analyzation
    	Bitmap test = Bitmap.createBitmap(bitmap, 0, 0, width, height);
    	while(data.remaining() > 0)
        {
    		int pos = data.position();
    		short depth = 0;
    		depth = data.get();
    		if(depthHist[depth] > 254){
    			bitmap.setPixel(pos%width , (int)Math.floor(pos/width), Color.RED);
    		}
    		if(data.position() == 153600)
    			Log.d("depthGen", "Debug: " + depthHist[depth]);
        }
    	

    }
}


