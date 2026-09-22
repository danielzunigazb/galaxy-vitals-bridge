import {
  AbsoluteFill,
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { loadFont as loadDisplay } from "@remotion/google-fonts/Anton";
import { loadFont as loadMono } from "@remotion/google-fonts/JetBrainsMono";
import { loadFont as loadSans } from "@remotion/google-fonts/SpaceGrotesk";

const { fontFamily: displayFont } = loadDisplay("normal", { weights: ["400"], subsets: ["latin"] });
const { fontFamily: monoFont } = loadMono("normal", { weights: ["600", "700"], subsets: ["latin"] });
const { fontFamily: sansFont } = loadSans("normal", { weights: ["500", "700"], subsets: ["latin"] });

export const Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill
      name="Intro background"
      style={{
        backgroundColor: "#0a0a0f",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: sansFont,
      }}
    >
      <Interactive.Div
        name="Kicker pill"
        style={{
          fontFamily: monoFont,
          fontWeight: 700,
          fontSize: 28,
          textTransform: "uppercase",
          letterSpacing: 2,
          color: "#0a0a0f",
          backgroundColor: "oklch(88% 0.17 95)",
          border: "4px solid #f7f4ff",
          boxShadow: "6px 6px 0 #f7f4ff",
          padding: "10px 28px",
          transform: "rotate(-2deg)",
          marginBottom: 48,
          opacity: interpolate(frame, [0, 0.5 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
          translate: interpolate(frame, [0, 0.5 * fps], ["0px -20px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        $ curl -s /status
      </Interactive.Div>

      <Interactive.Div
        name="Title"
        style={{
          fontFamily: displayFont,
          fontSize: 148,
          lineHeight: 1,
          textAlign: "center",
          color: "#f7f4ff",
          maxWidth: 1400,
          opacity: interpolate(frame, [0.3 * fps, 0.9 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
          scale: interpolate(frame, [0.3 * fps, 0.9 * fps], [0.92, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 200 }),
            output: "perceptual-scale",
          }),
        }}
      >
        GALAXY VITALS
        <br />
        <span style={{ color: "oklch(72% 0.24 350)" }}>BRIDGE</span>
      </Interactive.Div>

      <Interactive.Div
        name="Subtitle"
        style={{
          fontFamily: monoFont,
          fontWeight: 600,
          fontSize: 34,
          color: "#b3aec7",
          marginTop: 36,
          letterSpacing: 0.5,
          opacity: interpolate(frame, [1.1 * fps, 1.6 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        tu Galaxy Fit3, en vivo, en tu portfolio
      </Interactive.Div>
    </AbsoluteFill>
  );
};
