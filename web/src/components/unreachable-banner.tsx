import { Alert, AlertDescription } from "@/components/ui/alert";
import { WifiOff } from "lucide-react";

export function UnreachableBanner() {
  return (
    <Alert variant="destructive" className="border-red-500/30 bg-red-500/10">
      <WifiOff className="h-4 w-4" />
      <AlertDescription>
        Server unreachable. Check your connection and server URL.
      </AlertDescription>
    </Alert>
  );
}
