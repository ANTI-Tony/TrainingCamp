import { Response } from "express";
import { ApiError } from "./types";

export function sendError(
  res: Response,
  status: number,
  errorCode: string,
  message: string,
  details?: unknown
) {
  const body: ApiError = { errorCode, message };
  if (details !== undefined) body.details = details;
  return res.status(status).json(body);
}

export function parsePositiveInt(
  raw: unknown,
  fallback: number,
  opts?: { min?: number; max?: number }
) {
  const str = typeof raw === "string" ? raw : undefined;
  const val = str ? Number.parseInt(str, 10) : fallback;
  if (!Number.isFinite(val)) return fallback;
  const min = opts?.min ?? 0;
  const max = opts?.max ?? Number.MAX_SAFE_INTEGER;
  return Math.min(Math.max(val, min), max);
}

