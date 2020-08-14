package com.example.android.emojify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.vdx.designertoast.DesignerToast;

public class Emojifier {

    private static final String LOG_TAG = "Emojifier" ;

    private static final float EMOJI_SCALE_FACTOR = .8f;
    private static final double EYE_OPEN_THRESHOLD = 0.15;
    private static final double SMILING_THRESHOLD = 0.51;

    private enum Emoji{
        SMILE,
        CLOSED_EYE_SMILE,
        LEFT_WINK,
        RIGHT_WINK,
        FROWN,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_FROWN
    }

    private static boolean isRightEyeOpen;
    private static boolean isLeftEyeOpen;
    private static boolean isSmiling;

    public static Bitmap detectFacesAndOverlayEmoji(Context context, Bitmap picture){

        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(false)
                .build();

        Frame frame = new Frame.Builder().setBitmap(picture).build();

        SparseArray<Face> faces = detector.detect(frame);

        Bitmap emojiBitmap = null;
        Bitmap resultBitmap = picture;



        if(faces.size()<1){
            DesignerToast.Error(context, "No faces are detected", Gravity.BOTTOM, Toast.LENGTH_SHORT);
        }else{
            DesignerToast.Success(context, "There is/are " + faces.size() + " faces detected"
                    , Gravity.BOTTOM, Toast.LENGTH_SHORT);
            Log.d(LOG_TAG, "There is/are " + faces.size() + " faces detected");
            for (int i = 0; i < faces.size(); ++i) {
                Face face = faces.valueAt(i);
                Emoji emoji = whichEmoji(face, context);

                switch (emoji){
                    case SMILE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.smile);
                        break;
                    case FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.frown);
                        break;
                    case LEFT_WINK:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.leftwink);
                        break;
                    case RIGHT_WINK:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.rightwink);
                        break;
                    case LEFT_WINK_FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.leftwinkfrown);
                        break;
                    case RIGHT_WINK_FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.rightwinkfrown);
                        break;
                    case CLOSED_EYE_SMILE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.closed_smile);
                        break;
                    case CLOSED_EYE_FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.closed_frown);
                        break;
                    default:
                        emojiBitmap = null;
                        Toast.makeText(context, R.string.no_emoji, Toast.LENGTH_SHORT).show();
                }

                resultBitmap = addBitmapToFace(resultBitmap, emojiBitmap, face);
            }
        }
        detector.release();

        return resultBitmap;
    }

    private static Emoji whichEmoji(Face face, Context context){

        double smileProb = face.getIsSmilingProbability();
        double leftEyeOpenProb = face.getIsLeftEyeOpenProbability();
        double rightEyeOpenProb = face.getIsRightEyeOpenProbability();


                // Log all the probabilities
        Log.d(LOG_TAG, "getClassifications: smilingProb = " + smileProb);
        Log.d(LOG_TAG, "getClassifications: leftEyeOpenProb = "
                + leftEyeOpenProb);
        Log.d(LOG_TAG, "getClassifications: rightEyeOpenProb = "
                + rightEyeOpenProb);


        Emoji emoji = Emoji.SMILE;

        isSmiling = (smileProb > SMILING_THRESHOLD);
        isLeftEyeOpen = (leftEyeOpenProb > EYE_OPEN_THRESHOLD);
        isRightEyeOpen = (rightEyeOpenProb > EYE_OPEN_THRESHOLD);

        if(isSmiling){
            if(isLeftEyeOpen && isRightEyeOpen)
                emoji = Emoji.SMILE;
            else if(!isLeftEyeOpen && !isRightEyeOpen)
                emoji = Emoji.CLOSED_EYE_SMILE;
            else if(!isLeftEyeOpen && isRightEyeOpen)
                emoji = Emoji.LEFT_WINK;
            else if(isLeftEyeOpen && !isRightEyeOpen)
                emoji = Emoji.RIGHT_WINK;
        }else{
            if(isLeftEyeOpen && isRightEyeOpen)
                emoji = Emoji.FROWN;
            else if(!isLeftEyeOpen && !isRightEyeOpen)
                emoji = Emoji.CLOSED_EYE_FROWN;
            else if(!isLeftEyeOpen && isRightEyeOpen)
                emoji = Emoji.LEFT_WINK_FROWN;
            else if(isLeftEyeOpen && !isRightEyeOpen)
                emoji = Emoji.RIGHT_WINK_FROWN;
        }

        Log.d(LOG_TAG, "Emoji =  " + emoji.name());
        DesignerToast.Success(context, "Emoji =  " + emoji.name(), Gravity.BOTTOM, Toast.LENGTH_SHORT);

        return emoji;
    }

    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }


}
