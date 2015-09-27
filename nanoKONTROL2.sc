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
//      changes very slowly (sbutton_slow_factor = 0.1 times slower) around
//      the previous value;
//   2. if M (mute) is pressed, then the adjacent fader or knob does not react
//      to changes until the button is released; then it assumes a new value
//      in a discontinuous way;
//   3. if R (random) is pressed and adjacent knob of fader is touched at
//      the right value, then it assumes a random value from (0,1).
//
// The M button takes precedence over S, which in turn takes precedence over R.
//
//
// This class assumes that MIDI interface is already connected (through e.g.
// MIDIIn.connectAll) and you know the srcID number of the port your device is
// connected to. See example below.
// ----------------------------------------------------------------------------
//
// How to use it
// -------------
// Create a new instance of the class: n = NanoKONTROL2(server, \key).
// Access knobs and faders by n.faders and n.knobs, two-dimensional arrays, for
// which the first index denotes the scene number. You can treat each element
// of the arrays as a UGen at control rate by adding .kr message.  Use it in
// your SynthDefs. For instance, n.knobs[2][7].kr gives you current value of
// the 8th knob in the 3rd scene.
//
//
// Example (assuming you are happy with all default values):
//
// (
// o = Server.internal.options;
// Server.default = s = Server.internal.reboot;
// )
//
// MIDIClient.init;
// MIDIIn.connectAll;
// MIDIClient.sources;
// // suppose your device is connected to port 3:
// ~nK2srcID = MIDIClient.sources[3].uid; // this is your srcID number.
// MIDIFunc.trace;
//
// n = NanoKONTROL2(s, \NK2, srcID: ~nK2srcID);
//
// (
// Ndef(
//     \NK2_test, { | freq = 440 |
//         Out.ar(0, Pan2.ar(
//             SinOsc.ar( freq,
//                 mul: n.faders[0][3].kr.linexp(0,1,0.0005,1)
//             ),
//             pos: n.knobs[2][2].kr.linlin(0,1 ,-1,1) )
//         );
//     }
// );
// )
// Ndef(\NK2_test).pause;
// Ndef(\NK2_test).resume;
// Ndef(\NK2_test).free;
//
// n.free;
// s.quit;
//
// ----------------------------------------------------------------------------



NanoKONTROL2 {

    // variables and default values
    var
    <server, <key, <srcID, <num_of_scenes,
    <knobs_init_val, <faders_init_val,

    <>sbutton_slow_factor = 0.1,

    <>scene = 0,
    <knobs, <faders,

    <sbuttons, <mbuttons, <rbuttons,

    key_mididef;


    // change this only if you writing a class for
    // nanoKONTROL3 with 9 knobs and faders
    const
    nk2num = 8,
    knobs_note  = 16,
    faders_note = 0,
    sbutton_note = 32,
    mbutton_note = 48,
    rbutton_note = 64,
    scene_dim_note = 58,
    scene_inc_note = 59;



    *new { arg
        server,
        key = \nK2,
        srcID = 1310720,
        num_of_scenes = 4,
        knobs_init_val = 0.5,
        faders_init_val = 0,
        button_slow_factor = 0.1;

        ^super.new.initNanoKONTROL2(server, key,
            srcID, num_of_scenes,
            knobs_init_val, faders_init_val,
            button_slow_factor
        );
    }


    initNanoKONTROL2 { arg server, key,
            srcID, num_of_scenes,
            knobs_init_val, faders_init_val,
            button_slow_factor;


        knobs = Array.fill(num_of_scenes, {Array.fill(nk2num, { arg i; NanoKONTROL2Knob(server, knobs_init_val) }) });
        faders = Array.fill(num_of_scenes, {Array.fill(nk2num, { arg i; NanoKONTROL2Fader(server, faders_init_val) }) });

        sbuttons = Array.fill(nk2num, { arg i; NanoKONTROL2Button(0) });
        mbuttons = Array.fill(nk2num, { arg i; NanoKONTROL2Button(0) });
        rbuttons = Array.fill(nk2num, { arg i; NanoKONTROL2Button(0) });

        key_mididef = key ++ "mididef";

        // define MIDIdef
        MIDIdef.cc(key_mididef,  { arg val, cc, chan, src;

            // change the scene
            if ( (cc == scene_dim_note) || (cc == scene_inc_note) && (val == 127), {

                if ( (cc == scene_dim_note), {
                    scene = (scene - 1 ) % num_of_scenes;
                    }, {
                    scene = (scene + 1 ) % num_of_scenes;
                });


                num_of_scenes.do{ arg i;
                    nk2num.do{ arg j;
                        knobs[i][j].matched = 0;
                        faders[i][j].matched = 0;
                    };
                };

                (key ++ " current scene: " + scene).postln;
            });


            // S button
            if ( (cc >= sbutton_note) && (cc <= (sbutton_note + nk2num - 1)), {

                sbuttons[cc - sbutton_note].val = val.linlin(0,127,0,1);

                knobs[scene][cc - sbutton_note].tmpSval  = knobs[scene][cc - sbutton_note].prev;
                faders[scene][cc - sbutton_note].tmpSval = faders[scene][cc - sbutton_note].prev;

                if ( (val == 0), {
                    // then it must have been pressed and released just now
                    knobs[scene][cc - sbutton_note].matched = 0;
                    faders[scene][cc - sbutton_note].matched = 0;
                });

            });

            // M button
            if ( (cc >= mbutton_note) && (cc <= (mbutton_note + nk2num - 1)), {

                mbuttons[cc - mbutton_note].val = val.linlin(0,127,0,1);

            });

            // R button
            if ( (cc >= rbutton_note) && (cc <= (rbutton_note + nk2num - 1)), {
                rbuttons[cc - rbutton_note].val = val.linlin(0,127,0,1);

                if ( (val == 0), {
                    // then it must have been pressed and released just now
                    knobs[scene][cc - rbutton_note].matched = 0;
                    faders[scene][cc - rbutton_note].matched = 0;
                });
            });


            // knobs
            if ( (cc >= knobs_note) && (cc <= (knobs_note + nk2num - 1)), {

               if( ( knobs[scene][cc - knobs_note].matched == 1), {

                    var p = val.linlin(0,127,0,1);

                    if ( (rbuttons[cc - knobs_note].val == 1), {
                        // TODO: smooth the randomness a little bit
                        p = 1.0.rand;
                    });

                    if ( (sbuttons[cc - knobs_note].val == 1), {
                        p = val.linlin(0,127,
                            max(0, knobs[scene][cc - knobs_note].tmpSval - sbutton_slow_factor),
                            min(1, knobs[scene][cc - knobs_note].tmpSval + sbutton_slow_factor)
                        );
                    });

                    if ( (mbuttons[cc - knobs_note].val == 0), {
                        knobs[scene][cc - knobs_note].prev = p;
                    });

                }, {


                    if( ( (val + 3) >= knobs[scene][cc - knobs_note].prev.linlin(0,1,0,127) ) && ((val - 3) <= knobs[scene][cc - knobs_note].prev.linlin(0,1,0,127) ), {
                            knobs[scene][cc - knobs_note].matched = 1;
                    });

                });

                knobs[scene][cc - knobs_note].set_asprev;
            });


            // faders
            if ( (cc >= faders_note) && (cc <= (faders_note + nk2num - 1) ), {

               if( ( faders[scene][cc - faders_note].matched == 1), {

                    var p = val.linlin(0,127,0,1);

                    if ( (rbuttons[cc - faders_note].val == 1), {
                        p = 1.0.rand;
                    });

                    if ( (sbuttons[cc - faders_note].val == 1), {
                        p = val.linlin(0,127,
                                max(0, faders[scene][cc - faders_note].tmpSval - sbutton_slow_factor),
                                min(1, faders[scene][cc - faders_note].tmpSval + sbutton_slow_factor)
                        );
                    });

                    if ( (mbuttons[cc - faders_note].val == 0), {
                        faders[scene][cc - faders_note].prev = p;
                    });


                }, {

                    if( ( (val + 3) >= faders[scene][cc - faders_note].prev.linlin(0,1,0,127)) && ((val - 3) <= faders[scene][cc - faders_note].prev.linlin(0,1,0,127)), {
                            faders[scene][cc - faders_note].matched = 1;
                    });

                });

                faders[scene][cc - faders_note].set_asprev;

            });

        }, srcID: srcID);

    }


    free {
        MIDIdef(key_mididef).free;

        num_of_scenes.do{ arg i;
                    nk2num.do{ arg j;
                        knobs[i][j].free;
                        faders[i][j].free;
                    };
        };

        ^super.free;
    }

}



NanoKONTROL2Control {

    var
    server,
    init_val = 0,
    <val = 0, <>prev = 0, <>matched = 0,
    <bus,

    <>tmpSval = 0;

    *new { arg server, init_val;
        ^super.newCopyArgs(server, init_val).initNK2Control;
    }

    initNK2Control {
        prev = init_val;
        val = init_val;
        bus = Bus.control(server, 1);
        bus.set(init_val);
    }


    val_{ arg v;
        val = v;
        bus.set(v);
    }

    set_asprev {
        val = prev;
        bus.set(prev);
    }

    kr {
        ^bus.kr;
    }

    free {
        bus.free;
        ^super.free;
    }


}


// To be expanded later...

NanoKONTROL2Knob : NanoKONTROL2Control {}

NanoKONTROL2Fader : NanoKONTROL2Control {}


NanoKONTROL2Button {

    var <>val = 0;

    *new { |... args|
        ^super.newCopyArgs(*args);
    }

}

