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

export const Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill
      name="Outro background"
      style={{
        backgroundColor: "#0a0a0f",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: sansFont,
        gap: 28,
      }}
    >
      <Interactive.Div
        name="Outro url"
        style={{
          fontFamily: displayFont,
          fontSize: 96,
          color: "#f7f4ff",
          textAlign: "center",
          opacity: interpolate(frame, [0, 0.5 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
          scale: interpolate(frame, [0, 0.5 * fps], [0.9, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 200 }),
            output: "perceptual-scale",
          }),
        }}
      >
        danzuniga.xyz<span style={{ color: "oklch(72% 0.24 350)" }}>/status</span>
      </Interactive.Div>

      <Interactive.Div
        name="Outro repo pill"
        style={{
          fontFamily: monoFont,
          fontWeight: 700,
          fontSize: 26,
          color: "#0a0a0f",
          backgroundColor: "oklch(76% 0.21 150)",
          border: "3px solid #f7f4ff",
          boxShadow: "5px 5px 0 #f7f4ff",
          padding: "10px 24px",
          opacity: interpolate(frame, [0.7 * fps, 1.2 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        github.com/danielzunigazb/galaxy-vitals-bridge
      </Interactive.Div>

      <Interactive.Div
        name="Outro tagline"
        style={{
          fontFamily: monoFont,
          fontSize: 22,
          color: "#7a7591",
          marginTop: 20,
          opacity: interpolate(frame, [1.4 * fps, 1.9 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        construido con Claude Code
      </Interactive.Div>
    </AbsoluteFill>
  );
};
