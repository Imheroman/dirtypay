import { sessionHandlers } from './session';
import { organizationHandlers } from './organization';
import { roundHandlers } from './round';

export const handlers = [
  ...sessionHandlers,
  ...organizationHandlers,
  ...roundHandlers,
];
