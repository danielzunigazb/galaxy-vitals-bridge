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

// Same 12x14 sprite data as status.html's PORTRAIT_OPEN + "casa" outfit — the
// real pixel avatar from the actual site, not a stand-in.
const PALETTE: Record<string, string> = {
  ".": "transparent",
  H: "#241b2e",
  S: "#e8b48c",
  K: "#120d16",
  P: "#ff2d78",
  Z: "#f7f4ff",
};
const PORTRAIT_ROWS = [
  "..HHHHHHHH..",
  ".HHHHHHHHHH.",
  "HHHHHHHHHHHH",
  "HHSSSSSSSSHH",
  "HSSSSSSSSSSH",
  "SSSKSSSSKSSS",
  "SSSSSSSSSSSS",
  "SSSSSSSSSSSS",
  "..SSSSSSSS..",
  ".PPPPPPPPPP.",
  "PPPPPZZPPPPP",
  "PPPPPZZPPPPP",
  "PPPPPZZPPPPP",
  ".PPPPPPPPPP.",
];

const PixelAvatar: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "repeat(12, 1fr)",
        gridTemplateRows: "repeat(14, 1fr)",
        width: 280,
        height: 326,
        translate: interpolate(
          frame,
          [0, 1.3 * fps, 2.6 * fps],
          ["0px 0px", "0px -10px", "0px 0px"],
          { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
        ),
      }}
    >
      {PORTRAIT_ROWS.flatMap((row, rowIndex) =>
        [...row].map((ch, colIndex) => (
          <div key={`${rowIndex}-${colIndex}`} style={{ background: PALETTE[ch] }} />
        )),
      )}
    </div>
  );
};

// One beat shape doubled side by side (200-wide viewBox); shifting exactly
// 100 units per beat loops seamlessly. Driven by frame % beatFrames instead
// of a CSS animation, since Remotion renders frame-by-frame.
const EkgMonitor: React.FC<{ bpm: number }> = ({ bpm }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const beatFrames = (60 / bpm) * fps;
  const progress = (frame % beatFrames) / beatFrames;

  return (
    <div
      style={{
        width: 640,
        height: 160,
        overflow: "hidden",
        backgroundColor: "#06060a",
        border: "4px solid #f7f4ff",
        boxShadow: "6px 6px 0 oklch(62% 0.22 25)",
        position: "relative",
      }}
    >
      <svg
        viewBox="0 0 200 40"
        preserveAspectRatio="none"
        style={{
          position: "absolute",
          width: "200%",
          height: "100%",
          transform: `translateX(${-progress * 100}%)`,
        }}
      >
        <path
          d="M0,20 L10,20 L14,15 L18,20 L28,20 L31,22 L34,2 L37,34 L40,20 L48,20 L54,12 L60,20 L100,20 L110,20 L114,15 L118,20 L128,20 L131,22 L134,2 L137,34 L140,20 L148,20 L154,12 L160,20 L200,20"
          fill="none"
          stroke="#38ff8e"
          strokeWidth={2}
        />
      </svg>
    </div>
  );
};

export const StatusPage: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill
      name="Status page background"
      style={{
        backgroundColor: "#0a0a0f",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: sansFont,
        gap: 40,
      }}
    >
      <Interactive.Div
        name="Status title"
        style={{
          fontFamily: displayFont,
          fontSize: 72,
          color: "#f7f4ff",
          opacity: interpolate(frame, [0, 0.4 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        UN <span style={{ color: "oklch(76% 0.21 150)" }}>STATUS PAGE</span> VIVO
      </Interactive.Div>

      <div style={{ display: "flex", alignItems: "center", gap: 72 }}>
        <Interactive.Div
          name="Avatar block"
          style={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: 16,
            backgroundColor: "#15131c",
            border: "4px solid #f7f4ff",
            boxShadow: "8px 8px 0 oklch(72% 0.24 350)",
            padding: 28,
            opacity: interpolate(frame, [0.4 * fps, 0.9 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
            translate: interpolate(frame, [0.4 * fps, 0.9 * fps], ["-40px 0px", "0px 0px"], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
          }}
        >
          <div style={{ backgroundColor: "#0a0a0f", padding: 16, border: "3px solid #f7f4ff" }}>
            <PixelAvatar />
          </div>
          <div style={{ fontFamily: monoFont, fontWeight: 700, fontSize: 24, color: "#f7f4ff" }}>
            DANIEL ZUÑIGA
          </div>
          <div style={{ fontFamily: monoFont, fontSize: 18, color: "oklch(76% 0.21 150)" }}>🏠 EN CASA</div>
        </Interactive.Div>

        <Interactive.Div
          name="Vitals block"
          style={{
            display: "flex",
            flexDirection: "column",
            gap: 24,
            opacity: interpolate(frame, [0.7 * fps, 1.2 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
            translate: interpolate(frame, [0.7 * fps, 1.2 * fps], ["40px 0px", "0px 0px"], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
          }}
        >
          <EkgMonitor bpm={93} />
          <div style={{ display: "flex", gap: 20, fontFamily: monoFont, fontWeight: 700, fontSize: 26, color: "#f7f4ff" }}>
            <span>❤️ 93 BPM</span>
            <span>👟 6958 PASOS</span>
          </div>
        </Interactive.Div>
      </div>

      <Interactive.Div
        name="Status caption"
        style={{
          fontFamily: monoFont,
          fontWeight: 600,
          fontSize: 26,
          color: "#7a7591",
          opacity: interpolate(frame, [2.4 * fps, 2.9 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        latido real, zona real, "now playing" real — nada inventado
      </Interactive.Div>
    </AbsoluteFill>
  );
};
