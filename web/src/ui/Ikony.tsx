import { SvgIcon, type SvgIconProps } from "@mui/material";

// Te same ikony Material (Apache 2.0) co w aplikacji, bez całej biblioteki ikon

export const IkonaWstecz = (p: SvgIconProps) => (
  <SvgIcon {...p}>
    <path d="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z" />
  </SvgIcon>
);

export const IkonaWiecej = (p: SvgIconProps) => (
  <SvgIcon {...p}>
    <path d="M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z" />
  </SvgIcon>
);

export const IkonaDoSap = (p: SvgIconProps) => (
  <SvgIcon {...p}>
    <path d="M19,3h-4.18C14.4,1.84 13.3,1 12,1c-1.3,0 -2.4,0.84 -2.82,2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM12,3c0.55,0 1,0.45 1,1s-0.45,1 -1,1 -1,-0.45 -1,-1 0.45,-1 1,-1zM10,17l-4,-4 1.41,-1.41L10,14.17l6.59,-6.59L18,9l-8,8z" />
  </SvgIcon>
);

export const IkonaDodaj = (p: SvgIconProps) => (
  <SvgIcon {...p}>
    <path d="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
  </SvgIcon>
);
