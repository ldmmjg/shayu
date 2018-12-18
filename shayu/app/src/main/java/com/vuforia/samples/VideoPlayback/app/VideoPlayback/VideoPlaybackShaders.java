/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VideoPlayback.app.VideoPlayback;

public class VideoPlaybackShaders
{
    
    public static final String VIDEO_PLAYBACK_VERTEX_SHADER =
                    "attribute vec4 vertexPosition; \n"+
                    "attribute vec4 vertexNormal; \n"+
                    "attribute vec2 vertexTexCoord; \n"+
                    "varying vec2 texCoord; \n"+
                    "varying vec4 normal; \n"+
                    "uniform mat4 modelViewProjectionMatrix; \n"+
                    "void main() \n"+
                    "{ \n"+
                    "   gl_Position = modelViewProjectionMatrix * vertexPosition; \n"+
                    "   normal = vertexNormal; \n"+
                    "   texCoord = vertexTexCoord; \n"+
                    "} \n";


    public static  String FRAGMENT_SHADER_EXT_TRANS = "FD7F53AA4EC72776CFC78BD599FCB3C064EBFB2C42EF492E073500A9A455A5465ABCA5ED5DEC429E95821A838316701AEC3B8D2A11EAD272239C3D609E0DCE2A43377E692FB6F7C1386D1B140CC377755DF48D78C42DD1619C277EFF88EF7D4598EA82FA008626D6C32EB7773BEBD7A83355523F6AF5E60275F01A2B47AB71CFA5E54A066A4B4259478CC8CB7AD889A8FAEF52ACD6B01981732F389C8A6803BB593831E00B8A0DA4063031811837B54635E7E32616303963000EFF4674184B25343D93B4E5F4A082B0D0C05EC75FA046E4B7D0673634CE10A8DABEDC1325FB21A7E6DD8DC9957C4FDF9D6541009021988E22AF6064F168A4425F215EAA317D2C47B178B762770F9EFB69D39019603E31";

    /*
     * 
     * IMPORTANT:
     * 
     * The SurfaceTexture functionality from ICS provides the video frames from
     * the movie in an unconventional format. So we cant use Texture2D but we
     * need to use the ExternalOES extension.
     * 
     * Two things that are important in the shader below. The first is the
     * extension declaration (first line). The second is the type of the
     * texSamplerOES uniform.
     */

    public static final String VIDEO_PLAYBACK_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require \n" +
                    "precision mediump float; \n" +
                    "uniform samplerExternalOES texSamplerOES; \n" +
                    " varying vec2 texCoord;\n" +
                    " varying vec2 texdim0;\n" +
                    " void main()\n\n" +
                    " {\n" +
                    " vec3 keying_color = vec3(0.0, 0.0, 0.0);\n" +
                    " float thresh = 0.45; // [0, 1.732]\n" +
                    " float slope = 0.7157; // [0, 1]\n" +
                    " vec3 input_color = texture2D(texSamplerOES, texCoord).rgb;\n" +
                    " float d = abs(length(abs(keying_color.rgb - input_color.rgb)));\n" +
                    " float edge0 = thresh * (1.0 - slope);\n" +
                    " float alpha = smoothstep(edge0, thresh, d);\n" +
                    " gl_FragColor = vec4(input_color,alpha);\n" +
                    " }";


//    public static final String VIDEO_PLAYBACK_FRAGMENT_SHADER =
//            "#extension GL_OES_EGL_image_external : require \n" +
//                    "precision mediump float; \n" +
//                    "uniform samplerExternalOES texSamplerOES; \n" +
//                    " varying vec2 texCoord;\n" +
//                    " varying vec2 texdim0;\n" +
//                    " void main()\n\n" +
//                    " {\n" +
//                    " vec3 keying_color = vec3(0.647, 0.941, 0.29);\n" +
//                    " float thresh = 0.45; // [0, 1.732]\n" +
//                    " float slope = 0.1; // [0, 1]\n" +
//                    " vec3 input_color = texture2D(texSamplerOES, texCoord).rgb;\n" +
//                    " float d = abs(length(abs(keying_color.rgb - input_color.rgb)));\n" +
//                    " float edge0 = thresh * (1.0 - slope);\n" +
//                    " float alpha = smoothstep(edge0, thresh, d);\n" +
//                    " gl_FragColor = vec4(input_color,alpha);\n" +
//                    " }";

}
