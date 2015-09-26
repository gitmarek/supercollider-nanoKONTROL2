// ----------------------------------------------------------------------------
// KORG nanoKONTROL 2
//
// Read controller MIDI data and map them linearly (0,127) -> (0,1)
// to buses at control rate.
//
// If a scene changes (by pressing Track buttons) then the knobs or faders
// have to be set at the previous value for that scene, so they do not
// jump discontinuously.
// The meaning of buttons (S, M, R) is somewhat unusual:
//   1. if S (slow) is pressed, then the adjacent fader or knob reacts to
//      changes very slowly (~nK2_SBUTTON_SLOW_FACTOR = 0.1 times slower) around
//      the previous value;
//   2. if M (mute) is pressed, then the adjacent fader or knob does not react
//      to changes until the button is released; then it assumes a new value
//      in a discontinuous way;
//   3. if R (random) is pressed and adjacent knob of fader is touched at
//      the right value, then it assumes a random value from (0,1).
//
// The M button takes precedence over S, which in turn takes precedence over R.
// ----------------------------------------------------------------------------



// Initialise MIDI interface and connect to nK2:
(
MIDIClient.init;
MIDIIn.connectAll;
// MIDIFunc.trace;
)


// MIDI definitions
(
// parameters (change them if you like)
~nK2_NUM_OF_SCENES = 4;

~nK2_KNOBS_START_VAL = 0.5; // initialise all knobs with this value
~nK2_FADERS_START_VAL = 0;  // and all faders with this one

~nK2_SBUTTON_SLOW_FACTOR = 0.1; // a number between 0 and 1, denoting, how slow the value
                                // of a fader or knob changes, is S button is pressed.


// some constants (MIDI msg numbers etc.)
~nK2_NUM = 8;
~nK2_MAGSRC = 1310720; // My controller's magic source number:
                       // find yours by uncommenting MIDIFunc.tace; above.

~nK2_KNOBS_NOTE = 16;
~nK2_FADERS_NOTE = 0;
~nK2_SBUTTON_NOTE = 32;
~nK2_MBUTTON_NOTE = 48;
~nK2_RBUTTON_NOTE = 64;

~nK2_SCENE_DIM = 58;
~nK2_SCENE_INC = 59;


// some global variables
~nK2_knobs = Array.fill(~nK2_NUM_OF_SCENES, {Array.fill(~nK2_NUM, { arg i; Bus.control(s).set(~nK2_KNOBS_START_VAL) }) });
~nK2_faders = Array.fill(~nK2_NUM_OF_SCENES, {Array.fill(~nK2_NUM, { arg i; Bus.control(s).set(~nK2_FADERS_START_VAL) }) });

~nK2_scene = 0;


// initialise knobs and faders
~nK2_knobs_prev = Array.fill(~nK2_NUM_OF_SCENES, {Array.fill(~nK2_NUM, { arg i; ~nK2_KNOBS_START_VAL }) });
~nK2_faders_prev = Array.fill(~nK2_NUM_OF_SCENES, {Array.fill(~nK2_NUM, { arg i; ~nK2_FADERS_START_VAL }) });

~nK2_Sbuttons = Array.fill(~nK2_NUM, { arg i; 0 });
~nK2_Mbuttons = Array.fill(~nK2_NUM, { arg i; 0 });
~nK2_Rbuttons = Array.fill(~nK2_NUM, { arg i; 0 });

// internal stuff
~nK2_knobs_prev_matched = Array.fill(~nK2_NUM_OF_SCENES, {Array.fill(~nK2_NUM, { arg i; 0 }) });
~nK2_faders_prev_matched = Array.fill(~nK2_NUM_OF_SCENES, {Array.fill(~nK2_NUM, { arg i; 0 }) });
~nK2_tmpSval_knobs  = Array.fill(~nK2_NUM, { arg i; 0 });
~nK2_tmpSval_faders = Array.fill(~nK2_NUM, { arg i; 0 });



MIDIdef.cc(\nK2,
    { arg val, cc, chan, src;

        if( ( src == ~nK2_MAGSRC), {

            // change the scene
            if ( (cc == ~nK2_SCENE_DIM) || (cc == ~nK2_SCENE_INC) && (val == 127), {

                if ( (cc == ~nK2_SCENE_DIM), {
                    ~nK2_scene = (~nK2_scene - 1 ) % ~nK2_NUM_OF_SCENES;
                    }, {
                    ~nK2_scene = (~nK2_scene + 1 ) % ~nK2_NUM_OF_SCENES;
                });


               ~nK2_knobs_prev_matched = Array.fill(~nK2_NUM_OF_SCENES, {Array.fill(~nK2_NUM, { arg i; 0 }) });
               ~nK2_faders_prev_matched = Array.fill(~nK2_NUM_OF_SCENES, {Array.fill(~nK2_NUM, { arg i; 0 }) });

                ("nK2 current scene: " + ~nK2_scene).postln;
            });


            // S button
            if ( (cc >= ~nK2_SBUTTON_NOTE) && (cc <= (~nK2_SBUTTON_NOTE + ~nK2_NUM - 1)), {

                ~nK2_Sbuttons[cc - ~nK2_SBUTTON_NOTE] = val.linlin(0,127,0,1);

                ~nK2_tmpSval_knobs[cc - ~nK2_SBUTTON_NOTE]  =  ~nK2_knobs_prev[~nK2_scene][cc - ~nK2_SBUTTON_NOTE];
                ~nK2_tmpSval_faders[cc - ~nK2_SBUTTON_NOTE] = ~nK2_faders_prev[~nK2_scene][cc - ~nK2_SBUTTON_NOTE];

                if ( (val == 0), {
                    // then it must have been pressed and released just now
                    ~nK2_knobs_prev_matched[~nK2_scene][cc - ~nK2_SBUTTON_NOTE] = 0;
                    ~nK2_faders_prev_matched[~nK2_scene][cc - ~nK2_SBUTTON_NOTE] = 0;
                });

            });

            // M button
            if ( (cc >= ~nK2_MBUTTON_NOTE) && (cc <= (~nK2_MBUTTON_NOTE + ~nK2_NUM - 1)), {

                ~nK2_Mbuttons[cc - ~nK2_MBUTTON_NOTE] = val.linlin(0,127,0,1);

            });

            // R button
            if ( (cc >= ~nK2_RBUTTON_NOTE) && (cc <= (~nK2_RBUTTON_NOTE + ~nK2_NUM - 1)), {
                ~nK2_Rbuttons[cc - ~nK2_RBUTTON_NOTE] = val.linlin(0,127,0,1);

                if ( (val == 0), {
                    // then it must have been pressed and released just now
                    ~nK2_knobs_prev_matched[~nK2_scene][cc - ~nK2_RBUTTON_NOTE] = 0;
                    ~nK2_faders_prev_matched[~nK2_scene][cc - ~nK2_RBUTTON_NOTE] = 0;
                });
            });


            // knobs
            if ( (cc >= ~nK2_KNOBS_NOTE) && (cc <= (~nK2_KNOBS_NOTE + ~nK2_NUM - 1)), {

               if( ( ~nK2_knobs_prev_matched[~nK2_scene][cc - ~nK2_KNOBS_NOTE] == 1), {

                    var p = val.linlin(0,127,0,1);

                    if ( (~nK2_Rbuttons[cc - ~nK2_KNOBS_NOTE] == 1), {
                        // TODO: smooth the randomness a little bit
                        p = 1.0.rand;
                    });

                    if ( (~nK2_Sbuttons[cc - ~nK2_KNOBS_NOTE] == 1), {
                        p = val.linlin(0,127,
                            max(0, ~nK2_tmpSval_knobs[cc - ~nK2_KNOBS_NOTE] - ~nK2_SBUTTON_SLOW_FACTOR),
                            min(1, ~nK2_tmpSval_knobs[cc - ~nK2_KNOBS_NOTE] + ~nK2_SBUTTON_SLOW_FACTOR)
                        );
                    });

                    if ( (~nK2_Mbuttons[cc - ~nK2_KNOBS_NOTE] == 0), {
                        ~nK2_knobs_prev[~nK2_scene][cc - ~nK2_KNOBS_NOTE] = p;
                    });

                }, {

                    if( ( (val + 3) >= ~nK2_knobs_prev[~nK2_scene][cc - ~nK2_KNOBS_NOTE].linlin(0,1,0,127)) && ((val - 3) <= ~nK2_knobs_prev[~nK2_scene][cc - ~nK2_KNOBS_NOTE].linlin(0,1,0,127)), {
                            ~nK2_knobs_prev_matched[~nK2_scene][cc - ~nK2_KNOBS_NOTE] = 1;
                    });

                });

                ~nK2_knobs[~nK2_scene][cc - ~nK2_KNOBS_NOTE].set(~nK2_knobs_prev[~nK2_scene][cc - ~nK2_KNOBS_NOTE]);
            });


            // faders
            if ( (cc >= ~nK2_FADERS_NOTE) && (cc <= (~nK2_FADERS_NOTE + ~nK2_NUM - 1) ), {

               if( ( ~nK2_faders_prev_matched[~nK2_scene][cc - ~nK2_FADERS_NOTE] == 1), {

                    var p = val.linlin(0,127,0,1);

                    if ( (~nK2_Rbuttons[cc - ~nK2_FADERS_NOTE] == 1), {
                        p = 1.0.rand;
                    });

                    if ( (~nK2_Sbuttons[cc - ~nK2_FADERS_NOTE] == 1), {
                        p = val.linlin(0,127,
                            max(0, ~nK2_tmpSval_faders[cc - ~nK2_FADERS_NOTE] - ~nK2_SBUTTON_SLOW_FACTOR),
                            min(1, ~nK2_tmpSval_faders[cc - ~nK2_FADERS_NOTE] + ~nK2_SBUTTON_SLOW_FACTOR)
                        );
                    });

                    if ( (~nK2_Mbuttons[cc - ~nK2_FADERS_NOTE] == 0), {
                        ~nK2_faders_prev[~nK2_scene][cc - ~nK2_FADERS_NOTE] = p;
                    });


                }, {

                    if( ( (val + 3) >= ~nK2_faders_prev[~nK2_scene][cc - ~nK2_FADERS_NOTE].linlin(0,1,0,127)) && ((val - 3) <= ~nK2_faders_prev[~nK2_scene][cc - ~nK2_FADERS_NOTE].linlin(0,1,0,127)), {
                            ~nK2_faders_prev_matched[~nK2_scene][cc - ~nK2_FADERS_NOTE] = 1;
                    });

                });

                ~nK2_faders[~nK2_scene][cc - ~nK2_FADERS_NOTE].set(~nK2_faders_prev[~nK2_scene][cc - ~nK2_FADERS_NOTE]);

            });
        });
    }
)

)


// clear everything
MIDIdef(\nK2).free;


// Example:
//
// (
// Ndef(
//     \nK2_test, { | freq = 440 |
//         Out.ar(0, Pan2.ar(
//             SinOsc.ar( freq,
//                 mul: ~nK2_faders[0][3].kr.linexp(0,1,0.0005,1)
//             ),
//             pos: ~nK2_knobs[2][2].kr.linlin(0,1 ,-1,1) )
//         );
//     }
// );
// )
// Ndef(\nK2_test).pause;
// Ndef(\nK2_test).resume;
//


