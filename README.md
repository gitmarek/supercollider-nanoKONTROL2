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


## How to use it?
1. Open the file nanoKONTROL2.scd in SuperCollider.
2. Check, if `~nK2_MAGSRC` is set to a correct value.
3. Run the code!


## TODO
1. Find use for the rest of the buttons.
2. Rewrite the code as a class.
3. LEDs


## Authors
Marek Miller, <marek.l.miller@gmail.com>


## License
This sofware is under MIT license.  Feel free to use it!  For more information, see [LICENSE](./LICENSE).

