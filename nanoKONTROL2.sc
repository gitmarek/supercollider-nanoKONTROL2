// ----------------------------------------------------------------------------
// KORG nanoKONTROL 2
//
// See README.md for more information.
// -----------------------------------
//
// The MIT License (MIT)
//
// Copyright (c) 2015 Marek Miller
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
// ------------------------------------------------------------------------------



NanoKONTROL2 {

    // variables and default values
    var
    <server, <srcID, <num_of_scenes,
    <knobs_init_val, <faders_init_val,

    <>button_slow_factor,

    <>verbose,

    <>scene = 0,
    <knobs, <faders,

    <sbuttons, <mbuttons, <rbuttons,

    mididef_kf_key;


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
        srcID = 1310720,
        num_of_scenes = 4,

        knobs_init_val = 0,
        faders_init_val = 0,
        button_slow_factor = 0.1,
        verbose = 0;

        ^super.new.initNanoKONTROL2(server,
            srcID, num_of_scenes,
            knobs_init_val, faders_init_val,
            button_slow_factor,
            verbose
        );
    }


    initNanoKONTROL2 { arg arg_server,
            arg_srcID, arg_num_of_scenes,
            arg_knobs_init_val, arg_faders_init_val,
            arg_button_slow_factor, arg_verbose;


        server = arg_server;
        srcID = arg_srcID;
        num_of_scenes = arg_num_of_scenes;
        knobs_init_val  = arg_knobs_init_val;
        faders_init_val = arg_faders_init_val;
        button_slow_factor = arg_button_slow_factor;
        verbose = arg_verbose;

        knobs = Array.fill(num_of_scenes, {Array.fill(nk2num, { arg i; NanoKONTROL2Knob(server, knobs_init_val) }) });
        faders = Array.fill(num_of_scenes, {Array.fill(nk2num, { arg i; NanoKONTROL2Fader(server, faders_init_val) }) });

        sbuttons = Array.fill(nk2num, { arg i; NanoKONTROL2Button(0) });
        mbuttons = Array.fill(nk2num, { arg i; NanoKONTROL2Button(0) });
        rbuttons = Array.fill(nk2num, { arg i; NanoKONTROL2Button(0) });

        mididef_kf_key = "nK2_" ++ srcID.asString ++ "_default";
        this.nK2_kf_mididef(mididef_kf_key);
    }


    // ------------------------------------------------------------
    // define default MIDIdef for knobs and faders
    nK2_kf_mididef { arg key;

        MIDIdef.cc(key,  { arg val, cc, chan, src;

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

                ("nK2, srcID:" + srcID + ", current scene:" + scene + "/" + num_of_scenes ).postln;
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

                    var p = val.linlin(0,127,-1,1);

                    if ( (rbuttons[cc - knobs_note].val == 1), {
                        // TODO: smooth the randomness a little bit
                        p = 1.0.rand;
                    });

                    if ( (sbuttons[cc - knobs_note].val == 1), {
                        p = (val.linlin(0,127,-1,1) - knobs[scene][cc - knobs_note].tmpSval)*button_slow_factor +
                             knobs[scene][cc - knobs_note].tmpSval;
                        p = max(-1,p);
                        p = min(p, 1);
                    });

                    if ( (mbuttons[cc - knobs_note].val == 0), {
                        knobs[scene][cc - knobs_note].prev = p;

                       if( (this.verbose == 1), {
                            ("nK2, srcID: " ++ srcID.asString ++ ", scene: " ++ scene.asString ++ ", knob: " ++ (cc - knobs_note).asString ++ ", val: " ++ p).postln;
                       });
                    });

                }, {


                    if( ( (val + 3) >= knobs[scene][cc - knobs_note].prev.linlin(-1,1,0,127) ) && ((val - 3) <= knobs[scene][cc - knobs_note].prev.linlin(-1,1,0,127) ), {
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
                        p = (val.linlin(0,127,0,1) - faders[scene][cc - faders_note].tmpSval)*button_slow_factor +
                             faders[scene][cc - faders_note].tmpSval;
                        p = max(0,p);
                        p = min(p, 1);
                    });

                    if ( (mbuttons[cc - faders_note].val == 0), {
                        faders[scene][cc - faders_note].prev = p;

                        if( (this.verbose == 1), {
                            ("nK2, srcID: " ++ srcID.asString ++ ", scene: " ++ scene.asString ++ ", fader: " ++ (cc - faders_note).asString ++ ", val: " ++ p).postln;
                        });
                    });


                }, {

                    if( ( (val + 3) >= faders[scene][cc - faders_note].prev.linlin(0,1,0,127)) && ((val - 3) <= faders[scene][cc - faders_note].prev.linlin(0,1,0,127)), {
                            faders[scene][cc - faders_note].matched = 1;
                    });

                });

                faders[scene][cc - faders_note].set_asprev;

            });

        }, srcID: srcID);
    } // end of MIDIdef for knobs and faders
    // ------------------------------------------------------------


    dumpAll {

        ("nK2, srcID: " ++ srcID.asString).postln;

        num_of_scenes.do{ arg i;
            nk2num.do{ arg j;
                ("scene: " ++ i.asString ++ ", knob" ++ j.asString ++ ": " ++ knobs[i][j].val ++ ", fader" ++ j.asString ++ ": " ++ faders[i][j].val).postln;
            }
        }
    }


    free {
        MIDIdef(mididef_kf_key).free;

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
    <server,
    <init_val,
    <val, <>prev, <>matched = 0,
    <bus,

    <>tmpSval = 0;

    *new { arg server, init_val = 0;
        ^super.new.initNK2Control(server, init_val);
    }

    initNK2Control { arg arg_server, arg_init_val;
        server = arg_server;
        init_val = arg_init_val;

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

