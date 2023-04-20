import { registerPlugin } from '@capacitor/core';

import type { ResgridPlugin } from './definitions';

const Resgrid = registerPlugin<ResgridPlugin>('Resgrid', {
  web: () => import('./web').then(m => new m.ResgridWeb()),
});

export * from './definitions';
export { Resgrid };
