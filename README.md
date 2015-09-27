# supercollider-nanoKONTROL2
SuperCollider MIDI handlers for KORG nanoKONTROL2.

The code reads controller MIDI data and maps them linearly (0,127) -> (0,1)
to buses at control rate.

If a scene changes (by pressing Track buttons) to another one and then
changes back, then the knobs or faders have to be set at the previous value
for that scene, so they do not jump discontinuously.

The meaning of buttons (S, M, R) is somewhat unusual:

1. if S (slow) is pressed, then the adjacent fader or knob reacts to
   changes very slowly around the previous value;
2. if M (mute) is pressed, then the adjacent fader or knob does not react
   to changes until the button is released; then it assumes a new value
   in a discontinuous way;
3. if R (random) is pressed and adjacent knob of fader is touched at
   the right value, then it assumes a random value from (0,1).

The M button takes precedence over S, which in turn takes precedence over R.



## Installation
Put a copy of nanoKONTROLL2.sc in your SuperCollider extensions directory,
recompile class library (Ctrl+Shift+L).

## Usage
Create a new instance of the class: `n = NanoKONTROL2(server, \key)`.
Access knobs and faders by n.faders and n.knobs, two-dimensional arrays, for
which the first index denotes the scene number. You can treat each element
of the arrays as a UGen at control rate by adding .kr message.  Use it in
your SynthDefs. For instance, `n.knobs[2][7].kr` gives you current value of
the 8th knob in the 3rd scene.


### Example (assuming you are happy with all default values):
```SuperCollider

(
o = Server.internal.options;
Server.default = s = Server.internal.reboot;
)

MIDIClient.init;
MIDIIn.connectAll;
MIDIClient.sources;
// suppose your device is connected to port 3:
~nK2srcID = MIDIClient.sources[3].uid; // this is your srcID number.
MIDIFunc.trace;

n = NanoKONTROL2(s, \NK2, srcID: ~nK2srcID);

(
Ndef(
     \NK2_test, { | freq = 440 |
         Out.ar(0, Pan2.ar(
             SinOsc.ar( freq,
                 mul: n.faders[0][3].kr.linexp(0,1,0.0005,1)
             ),
             pos: n.knobs[2][2].kr.linlin(0,1 ,-1,1) )
         );
     }
);
)
Ndef(\NK2_test).pause;
Ndef(\NK2_test).resume;
Ndef(\NK2_test).free;

n.free;
s.quit;
```


## TODO
1. Find use for the rest of the buttons.
2. LEDs


## Authors
Marek Miller, <marek.l.miller@gmail.com>


## License
This software is under MIT license.  Feel free to use it!  For more information, see [LICENSE](./LICENSE).

